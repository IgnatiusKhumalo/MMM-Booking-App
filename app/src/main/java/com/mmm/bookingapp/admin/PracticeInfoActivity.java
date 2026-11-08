package com.mmm.bookingapp.admin;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mmm.bookingapp.R;
import com.mmm.bookingapp.models.PracticeInfo;
import com.mmm.bookingapp.utils.FirebaseRefs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
 *
 * Availability beyond opening hours:
 *  - Daily breaks (for example lunch): added with two time pickers. No
 *    appointment can overlap a break, and a break must sit inside the
 *    opening hours and not overlap another break.
 *  - Blocked dates and leave: added with two date pickers (the same date
 *    twice is a single day) plus an optional reason. Clients cannot book on
 *    those dates.
 * Both lists are edited on screen and saved together with everything else
 * when the admin taps Save Changes.
 */
public class PracticeInfoActivity extends AppCompatActivity {

    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private interface TimeChosen { void onChosen(int hour, int minute); }
    private interface DateChosen { void onChosen(Calendar date); }

    private EditText etPracticePhone, etPracticeEmail, etWhatsappLink,
            etHoursStart, etHoursEnd, etCrisisNote;
    private CheckBox[] dayCheckBoxes;   // same order as PracticeInfo.DAY_KEYS (Monday first)
    private TextView tvLocationsReadonly;
    private Button btnSave;
    private ProgressBar progressBar;

    private LinearLayout layoutBreaks, layoutBlocked;
    private TextView tvBreaksEmpty, tvBlockedEmpty;
    private final List<Map<String, String>> breaks = new ArrayList<>();
    private final List<Map<String, String>> blockedPeriods = new ArrayList<>();

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

        layoutBreaks = findViewById(R.id.layoutBreaks);
        layoutBlocked = findViewById(R.id.layoutBlocked);
        tvBreaksEmpty = findViewById(R.id.tvBreaksEmpty);
        tvBlockedEmpty = findViewById(R.id.tvBlockedEmpty);

        dayCheckBoxes = new CheckBox[]{
                findViewById(R.id.cbDayMonday), findViewById(R.id.cbDayTuesday),
                findViewById(R.id.cbDayWednesday), findViewById(R.id.cbDayThursday),
                findViewById(R.id.cbDayFriday), findViewById(R.id.cbDaySaturday),
                findViewById(R.id.cbDaySunday)};

        findViewById(R.id.btnAddBreak).setOnClickListener(v -> startBreakFlow());
        findViewById(R.id.btnAddBlocked).setOnClickListener(v -> startBlockedFlow());

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

                    breaks.clear();
                    if (info.getBreaks() != null) {
                        for (Map<String, String> b : info.getBreaks()) {
                            if (b != null) breaks.add(new HashMap<>(b));
                        }
                    }
                    blockedPeriods.clear();
                    if (info.getBlockedPeriods() != null) {
                        for (Map<String, String> p : info.getBlockedPeriods()) {
                            if (p != null) blockedPeriods.add(new HashMap<>(p));
                        }
                    }
                    renderBreaks();
                    renderBlocked();
                });
    }

    // ---------- Daily breaks ----------

    private void startBreakFlow() {
        pickTime(R.string.title_break_start, (startHour, startMinute) ->
                pickTime(R.string.title_break_end, (endHour, endMinute) ->
                        addBreak(startHour * 60 + startMinute, endHour * 60 + endMinute)));
    }

    private void pickTime(int titleRes, TimeChosen onChosen) {
        TimePickerDialog dialog = new TimePickerDialog(this,
                (view, hour, minute) -> onChosen.onChosen(hour, minute), 12, 0, true);
        dialog.setTitle(titleRes);
        dialog.show();
    }

    private void addBreak(int startMinutes, int endMinutes) {
        if (endMinutes <= startMinutes) {
            Toast.makeText(this, R.string.error_break_order, Toast.LENGTH_LONG).show();
            return;
        }

        int open = PracticeInfo.parseMinutes(etHoursStart.getText().toString().trim(), -1);
        int close = PracticeInfo.parseMinutes(etHoursEnd.getText().toString().trim(), -1);
        if (open >= 0 && close >= 0 && (startMinutes < open || endMinutes > close)) {
            Toast.makeText(this, getString(R.string.error_break_outside_hours,
                    PracticeInfo.formatMinutes(open), PracticeInfo.formatMinutes(close)), Toast.LENGTH_LONG).show();
            return;
        }

        for (Map<String, String> existing : breaks) {
            int s = PracticeInfo.parseMinutes(existing.get("start"), -1);
            int e = PracticeInfo.parseMinutes(existing.get("end"), -1);
            if (startMinutes < e && s < endMinutes) {
                Toast.makeText(this, R.string.error_break_overlap, Toast.LENGTH_LONG).show();
                return;
            }
        }

        Map<String, String> entry = new HashMap<>();
        entry.put("start", PracticeInfo.formatMinutes(startMinutes));
        entry.put("end", PracticeInfo.formatMinutes(endMinutes));
        breaks.add(entry);
        Collections.sort(breaks, (a, b) -> Integer.compare(
                PracticeInfo.parseMinutes(a.get("start"), 0), PracticeInfo.parseMinutes(b.get("start"), 0)));
        renderBreaks();
    }

    private void renderBreaks() {
        layoutBreaks.removeAllViews();
        tvBreaksEmpty.setVisibility(breaks.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < breaks.size(); i++) {
            final int index = i;
            Map<String, String> b = breaks.get(i);
            addRow(layoutBreaks, b.get("start") + " \u2013 " + b.get("end"), () -> {
                breaks.remove(index);
                renderBreaks();
            });
        }
    }

    // ---------- Blocked dates and leave ----------

    private void startBlockedFlow() {
        pickDate(R.string.title_blocked_from, null, from ->
                pickDate(R.string.title_blocked_to, from, to -> askReason(from, to)));
    }

    /** minDate is null for the first date (today onwards); for the second it is the first date. */
    private void pickDate(int titleRes, Calendar minDate, DateChosen onChosen) {
        Calendar initial = minDate != null ? (Calendar) minDate.clone() : Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.clear();
            chosen.set(year, month, day);
            onChosen.onChosen(chosen);
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(minDate != null ? minDate.getTimeInMillis() : System.currentTimeMillis());
        dialog.setTitle(titleRes);
        dialog.show();
    }

    private void askReason(Calendar from, Calendar to) {
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout container = new LinearLayout(this);
        container.setPadding(pad, pad / 2, pad, 0);
        EditText input = new EditText(this);
        input.setHint(R.string.hint_reason);
        input.setSingleLine(true);
        container.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_reason_title)
                .setView(container)
                .setPositiveButton(R.string.btn_add, (dialog, which) ->
                        addBlockedPeriod(from, to, input.getText().toString().trim()))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void addBlockedPeriod(Calendar from, Calendar to, String reason) {
        Map<String, String> entry = new HashMap<>();
        entry.put("from", toIso(from));
        entry.put("to", toIso(to));
        if (!reason.isEmpty()) entry.put("reason", reason);
        blockedPeriods.add(entry);
        Collections.sort(blockedPeriods, (a, b) -> a.get("from").compareTo(b.get("from")));
        renderBlocked();
    }

    private void renderBlocked() {
        layoutBlocked.removeAllViews();
        tvBlockedEmpty.setVisibility(blockedPeriods.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < blockedPeriods.size(); i++) {
            final int index = i;
            Map<String, String> p = blockedPeriods.get(i);
            String from = p.get("from");
            String to = p.get("to");
            String text = toDisplay(from);
            if (to != null && !to.isEmpty() && !to.equals(from)) text += " \u2013 " + toDisplay(to);
            String reason = p.get("reason");
            if (reason != null && !reason.isEmpty()) text += "  \u00B7  " + reason;
            addRow(layoutBlocked, text, () -> {
                blockedPeriods.remove(index);
                renderBlocked();
            });
        }
    }

    // ---------- Shared helpers ----------

    private void addRow(LinearLayout container, String text, Runnable onRemove) {
        View row = getLayoutInflater().inflate(R.layout.item_admin_list_row, container, false);
        ((TextView) row.findViewById(R.id.tvRowText)).setText(text);
        row.findViewById(R.id.tvRowRemove).setOnClickListener(v -> onRemove.run());
        container.addView(row);
    }

    private static String toIso(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    /** "yyyy-MM-dd" to "dd/MM/yyyy" for display. */
    private static String toDisplay(String iso) {
        if (iso == null || iso.length() != 10) return iso == null ? "" : iso;
        return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
    }

    // ---------- Save ----------

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
        int openMinutes = PracticeInfo.parseMinutes(hoursStart, 0);
        int closeMinutes = PracticeInfo.parseMinutes(hoursEnd, 0);
        if (openMinutes >= closeMinutes) {
            Toast.makeText(this, R.string.error_practice_hours_order, Toast.LENGTH_LONG).show();
            return;
        }
        // The hours may have been changed after breaks were added: every break must still fit inside them.
        for (Map<String, String> b : breaks) {
            int s = PracticeInfo.parseMinutes(b.get("start"), -1);
            int e = PracticeInfo.parseMinutes(b.get("end"), -1);
            if (s < openMinutes || e > closeMinutes) {
                Toast.makeText(this, getString(R.string.error_break_outside_hours, hoursStart, hoursEnd),
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        setLoading(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("phone", etPracticePhone.getText().toString().trim());
        updates.put("email", etPracticeEmail.getText().toString().trim());
        updates.put("whatsappLink", etWhatsappLink.getText().toString().trim());
        updates.put("openDays", openDays);
        updates.put("operatingDays", PracticeInfo.buildDisplayDays(openDays));
        updates.put("crisisLineNote", etCrisisNote.getText().toString().trim());
        updates.put("breaks", breaks);
        updates.put("blockedPeriods", blockedPeriods);

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