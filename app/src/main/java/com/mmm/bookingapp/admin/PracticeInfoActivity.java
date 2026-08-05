package com.mmm.bookingapp.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mmm.bookingapp.R;
import com.mmm.bookingapp.models.PracticeInfo;
import com.mmm.bookingapp.utils.FirebaseRefs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen 20 - Practice Information Screen (Admin).
 * Edits the shared practiceInfo/main document, which every client-facing
 * screen reads from - per the Blueprint, this means an update here takes
 * effect app-wide immediately, with no app rebuild required. Write access
 * is enforced admin-only server-side by Firestore Security Rules.
 */
public class PracticeInfoActivity extends AppCompatActivity {

    private EditText etPracticePhone, etPracticeEmail, etWhatsappLink,
            etOperatingDays, etHoursStart, etHoursEnd, etCrisisNote;
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
        etOperatingDays = findViewById(R.id.etOperatingDays);
        etHoursStart = findViewById(R.id.etHoursStart);
        etHoursEnd = findViewById(R.id.etHoursEnd);
        etCrisisNote = findViewById(R.id.etCrisisNote);
        tvLocationsReadonly = findViewById(R.id.tvLocationsReadonly);
        btnSave = findViewById(R.id.btnSavePracticeInfo);
        progressBar = findViewById(R.id.progressBar);

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
                    etOperatingDays.setText(info.getOperatingDays());
                    etCrisisNote.setText(info.getCrisisLineNote());

                    if (info.getOperatingHours() != null) {
                        etHoursStart.setText(info.getOperatingHours().getStart());
                        etHoursEnd.setText(info.getOperatingHours().getEnd());
                    }

                    List<String> locations = info.getLocations();
                    tvLocationsReadonly.setText(locations != null ? String.join(", ", locations) : "");
                });
    }

    private void savePracticeInfo() {
        setLoading(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("phone", etPracticePhone.getText().toString().trim());
        updates.put("email", etPracticeEmail.getText().toString().trim());
        updates.put("whatsappLink", etWhatsappLink.getText().toString().trim());
        updates.put("operatingDays", etOperatingDays.getText().toString().trim());
        updates.put("crisisLineNote", etCrisisNote.getText().toString().trim());

        Map<String, Object> hours = new HashMap<>();
        hours.put("start", etHoursStart.getText().toString().trim());
        hours.put("end", etHoursEnd.getText().toString().trim());
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