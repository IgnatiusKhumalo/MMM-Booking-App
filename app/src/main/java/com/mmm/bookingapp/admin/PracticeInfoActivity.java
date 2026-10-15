package com.mmm.bookingapp.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mmm.bookingapp.R;
import com.mmm.bookingapp.models.PracticeInfo;
import com.mmm.bookingapp.utils.FirebaseRefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Screen 20 - Practice Information Screen (Admin).
 * Edits the shared practiceInfo/main document, which every client-facing
 * screen reads - an update here takes effect app-wide immediately, with
 * no app rebuild. Write access is admin-only, enforced server-side by
 * Firestore Security Rules.
 *
 * Open days are chosen with checkboxes and saved as a structured list
 * (openDays), which the booking screen enforces. The readable text shown
 * on the Contact screen (operatingDays) is generated from that list.
 * Opening and closing times are validated as HH:mm with opening earlier
 * than closing before anything is saved.
 */
public class PracticeInfoActivity extends AppCompatActivity {

    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private EditText etPracticePhone, etPracticeEmail, etWhatsappLink,
            etHoursStart, etHoursEnd, etCrisisNote;
    private CheckBox[] dayCheckBoxes;   // same order as PracticeInfo.DAY_KEYS (Monday first)
    private TextView tvLocationsReadonly;
    private Button btnSave;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice_info);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        etPracticePhone = findViewById(R.id.etPracticePhone);
        etPracticeEmail = findViewById(R.id.etPracticeEmail);
        etWhatsappLink = findViewById(R.id.etWhatsappLink);
        etHoursStart = findViewById(R.id.etHoursStart);
        etHoursEnd = findViewById(R.id.etHoursEnd);
        etCrisisNote = findViewById(R.id.etCrisisNote);
        tvLocationsReadonly = findViewById(R.id.tvLocationsReadonly);
        btnSave = findViewById(R.id.btnSavePracticeInfo);
        progressBar = findViewById(R.id.progressBar);

        dayCheckBoxes = new CheckBox[]{
                findViewById(R.id.cbDayMonday), findViewById(R.id.cbDayTuesday),
                findViewById(R.id.cbDayWednesday), findViewById(R.id.cbDayThursday),
                findViewById(R.id.cbDayFriday), findViewById(R.id.cbDaySaturday),
                findViewById(R.id.cbDaySunday)};

        loadPracticeInfo();
        btnSave.setOnClickListener(v -> savePracticeInfo());
    }

    private void loadPracticeInfo() {
        FirebaseRefs.practiceInfoDoc().get()
                .addOnSuccessListener(document -> {
                    PracticeInfo info = document.exists() ? document.toObject(PracticeInfo.class) : null;
                    if (info == null) info = PracticeInfo.getFallback();

                    etPracticePhone.setText(info.getPhone());
                    etPracticeEmail.setText(info.getEmail());
                    etWhatsappLink.setText(info.getWhatsappLink());
                    etCrisisNote.setText(info.getCrisisLineNote());

                    List<String> openDays = info.effectiveOpenDays();
                    for (int i = 0; i < dayCheckBoxes.length; i++) {
                        dayCheckBoxes[i].setChecked(openDays.contains(PracticeInfo.DAY_KEYS[i]));
                    }

                    if (info.getOperatingHours() != null) {
                        etHoursStart.setText(info.getOperatingHours().getStart());
                        etHoursEnd.setText(info.getOperatingHours().getEnd());
                    }

                    List<String> locations = info.getLocations();
                    tvLocationsReadonly.setText(locations != null ? String.join(", ", locations) : "");
                });
    }

    private void savePracticeInfo() {
        String hoursStart = etHoursStart.getText().toString().trim();
        String hoursEnd = etHoursEnd.getText().toString().trim();

        List<String> openDays = new ArrayList<>();
        for (int i = 0; i < dayCheckBoxes.length; i++) {
            if (dayCheckBoxes[i].isChecked()) openDays.add(PracticeInfo.DAY_KEYS[i]);
        }

        if (openDays.isEmpty()) {
            Toast.makeText(this, R.string.error_practice_no_open_days, Toast.LENGTH_LONG).show();
            return;
        }
        if (!TIME_PATTERN.matcher(hoursStart).matches() || !TIME_PATTERN.matcher(hoursEnd).matches()) {
            Toast.makeText(this, R.string.error_practice_hours_invalid, Toast.LENGTH_LONG).show();
            return;
        }
        if (PracticeInfo.parseMinutes(hoursStart, 0) >= PracticeInfo.parseMinutes(hoursEnd, 0)) {
            Toast.makeText(this, R.string.error_practice_hours_order, Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("phone", etPracticePhone.getText().toString().trim());
        updates.put("email", etPracticeEmail.getText().toString().trim());
        updates.put("whatsappLink", etWhatsappLink.getText().toString().trim());
        updates.put("openDays", openDays);
        updates.put("operatingDays", PracticeInfo.buildDisplayDays(openDays));
        updates.put("crisisLineNote", etCrisisNote.getText().toString().trim());

        Map<String, Object> hours = new HashMap<>();
        hours.put("start", hoursStart);
        hours.put("end", hoursEnd);
        updates.put("operatingHours", hours);

        FirebaseRefs.practiceInfoDoc().update(updates)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, R.string.practice_info_updated_message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, getString(R.string.error_practice_info_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }
}