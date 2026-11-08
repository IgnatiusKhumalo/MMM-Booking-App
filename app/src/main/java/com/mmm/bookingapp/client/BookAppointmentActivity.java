package com.mmm.bookingapp.client;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mmm.bookingapp.R;
import com.mmm.bookingapp.models.BookedSlot;
import com.mmm.bookingapp.models.Booking;
import com.mmm.bookingapp.models.PracticeInfo;
import com.mmm.bookingapp.utils.AvailabilityUtils;
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.HolidayApiClient;
import com.mmm.bookingapp.utils.NetworkStatusHelper;
import com.mmm.bookingapp.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Screen 8 - Book Appointment Screen.
 *
 * Choosing a time: after picking a date, the client taps the time field and
 * sees a list of the FREE start times for that date, duration and location
 * (AvailabilityUtils) - times already reserved, in the past (today), outside
 * opening hours, or inside a break are not offered. If nothing is free the
 * client is told to try another date, duration or location. Changing the
 * date, duration, consultation type or location clears an already chosen
 * time, because the free times depend on all four.
 *
 * Booking rules enforced here:
 *  - The chosen day must be one of the practice's saved open days, must not
 *    fall in a blocked period (leave or closure set by the admin) and must
 *    not be a South African public holiday (Nager.Date API, cached per year).
 *  - The appointment must fit completely inside operating hours and must not
 *    overlap a break: start >= opening AND start + duration <= closing.
 *  - Start times are on the quarter hour, matching the 15-minute slot
 *    blocks that prevent overlaps.
 *  - No overlap with ANY other booking (the client's own or anyone
 *    else's): the booking reserves one slot document per 15-minute block
 *    inside a single Firestore transaction that fails if any block
 *    already exists. Each location (and Online) is its own calendar. The
 *    free-times list is a convenience; this transaction is the final
 *    safeguard if two people choose the same time at the same moment.
 *
 * For In-Person bookings the full clinic address of the selected location
 * is shown beneath the location dropdown (display only - the booking
 * itself stores just the location name).
 */
public class BookAppointmentActivity extends AppCompatActivity {

    private static final String TAG = "BookAppointment";

    private Spinner spinnerService, spinnerConsultationType, spinnerLocation, spinnerDuration;
    private View layoutLocation;
    private TextView tvOnlineNote, tvSelectedDate, tvSelectedTime, tvLocationAddress;
    private EditText etAdditionalInfo;
    private Button btnSubmitBooking;
    private ProgressBar progressBar;

    private String selectedDate = null;   // "yyyy-MM-dd"
    private String selectedTime = null;   // "HH:mm"
    private final int[] durationValues = {30, 45, 60};
    private String[] locationAddresses;   // same order as the physical_locations array

    private PracticeInfo practiceInfo; // loaded once; the fallback is used until it arrives
    private final Map<Integer, Set<String>> holidaysByYear = new HashMap<>(); // year -> "yyyy-MM-dd" dates

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        NotificationHelper.requestNotificationPermission(this);
        NetworkStatusHelper.observeConnectivity(this, findViewById(R.id.tvOfflineBanner));

        spinnerService = findViewById(R.id.spinnerService);
        spinnerConsultationType = findViewById(R.id.spinnerConsultationType);
        spinnerLocation = findViewById(R.id.spinnerLocation);
        spinnerDuration = findViewById(R.id.spinnerDuration);
        layoutLocation = findViewById(R.id.layoutLocation);
        tvOnlineNote = findViewById(R.id.tvOnlineNote);
        tvLocationAddress = findViewById(R.id.tvLocationAddress);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        etAdditionalInfo = findViewById(R.id.etAdditionalInfo);
        btnSubmitBooking = findViewById(R.id.btnSubmitBooking);
        progressBar = findViewById(R.id.progressBar);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        setupSpinners();
        loadPracticeInfo();
        ensureHolidaysLoaded(Calendar.getInstance().get(Calendar.YEAR), null);

        tvSelectedDate.setOnClickListener(v -> showDatePicker());
        tvSelectedTime.setOnClickListener(v -> showFreeTimes());
        btnSubmitBooking.setOnClickListener(v -> attemptSubmit());
    }

    // ---------- Setup ----------

    private void setupSpinners() {
        ArrayAdapter<CharSequence> serviceAdapter = ArrayAdapter.createFromResource(
                this, R.array.services_list, android.R.layout.simple_spinner_item);
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerService.setAdapter(serviceAdapter);

        ArrayAdapter<CharSequence> consultationAdapter = ArrayAdapter.createFromResource(
                this, R.array.consultation_types, android.R.layout.simple_spinner_item);
        consultationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerConsultationType.setAdapter(consultationAdapter);

        ArrayAdapter<CharSequence> locationAdapter = ArrayAdapter.createFromResource(
                this, R.array.physical_locations, android.R.layout.simple_spinner_item);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(locationAdapter);

        ArrayAdapter<CharSequence> durationAdapter = ArrayAdapter.createFromResource(
                this, R.array.durations_display, android.R.layout.simple_spinner_item);
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDuration.setAdapter(durationAdapter);

        spinnerConsultationType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                boolean isInPerson = position == 1; // 0 = Online, 1 = In-Person
                layoutLocation.setVisibility(isInPerson ? View.VISIBLE : View.GONE);
                tvOnlineNote.setVisibility(isInPerson ? View.GONE : View.VISIBLE);
                resetTimeSelection();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Show the full clinic address of whichever location is selected.
        locationAddresses = getResources().getStringArray(R.array.location_addresses);
        spinnerLocation.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                showAddressFor(position);
                resetTimeSelection();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerDuration.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                resetTimeSelection();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    /** Displays the address for the location at this position (same order as physical_locations). */
    private void showAddressFor(int position) {
        if (position >= 0 && position < locationAddresses.length) {
            tvLocationAddress.setText(locationAddresses[position]);
        } else {
            tvLocationAddress.setText("");
        }
    }

    private void loadPracticeInfo() {
        FirebaseRefs.practiceInfoDoc().get()
                .addOnSuccessListener(document -> {
                    PracticeInfo info = document.exists() ? document.toObject(PracticeInfo.class) : null;
                    practiceInfo = (info != null) ? info : PracticeInfo.getFallback();
                })
                .addOnFailureListener(e -> practiceInfo = PracticeInfo.getFallback());
    }

    private PracticeInfo currentPracticeInfo() {
        return practiceInfo != null ? practiceInfo : PracticeInfo.getFallback();
    }

    private int currentDuration() {
        return durationValues[spinnerDuration.getSelectedItemPosition()];
    }

    /**
     * The free times depend on the date, duration, consultation type and
     * location, so changing any of them clears a time that was already chosen.
     */
    private void resetTimeSelection() {
        if (selectedTime == null) return;
        selectedTime = null;
        tvSelectedTime.setText(R.string.tap_to_select_time);
        Toast.makeText(this, R.string.time_cleared_availability, Toast.LENGTH_LONG).show();
    }

    // ---------- Public holidays (cached per year) ----------

    /** Runs afterLoaded (may be null) once holidays for that year are cached, or once the fetch fails. */
    private void ensureHolidaysLoaded(int year, Runnable afterLoaded) {
        if (holidaysByYear.containsKey(year)) {
            if (afterLoaded != null) afterLoaded.run();
            return;
        }
        HolidayApiClient.fetchHolidaysForYear(year, new HolidayApiClient.HolidayCallback() {
            @Override
            public void onHolidaysLoaded(Set<String> holidayDates) {
                holidaysByYear.put(year, holidayDates);
                Log.i(TAG, "Public holidays loaded for " + year + ": " + holidayDates.size() + " dates");
                if (afterLoaded != null) afterLoaded.run();
            }

            @Override
            public void onError(String message) {
                // Not cached, so a later pick retries. Booking still works on the open-day and hours rules.
                Log.w(TAG, "Public holiday fetch failed for " + year + ": " + message);
                Toast.makeText(BookAppointmentActivity.this, R.string.holidays_load_failed, Toast.LENGTH_SHORT).show();
                if (afterLoaded != null) afterLoaded.run();
            }
        });
    }

    // ---------- Date selection ----------

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> handleDatePicked(year, month, dayOfMonth),
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    private void handleDatePicked(int year, int month, int dayOfMonth) {
        Calendar picked = Calendar.getInstance();
        picked.clear();
        picked.set(year, month, dayOfMonth);

        PracticeInfo info = currentPracticeInfo();
        if (!info.isOpenOn(picked.get(Calendar.DAY_OF_WEEK))) {
            Log.d(TAG, "Date rejected - practice closed that day of the week");
            Toast.makeText(this, getString(R.string.error_closed_day, info.describeOpenDays()), Toast.LENGTH_LONG).show();
            return;
        }

        String candidateDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
        String blockedReason = info.blockedReasonFor(candidateDate);
        if (blockedReason != null) {
            Log.d(TAG, "Date rejected - blocked period (leave or closure): " + candidateDate);
            Toast.makeText(this,
                    blockedReason.isEmpty()
                            ? getString(R.string.error_blocked_date_no_reason)
                            : getString(R.string.error_blocked_date, blockedReason),
                    Toast.LENGTH_LONG).show();
            return;
        }

        ensureHolidaysLoaded(year, () -> finishDateSelection(year, month, dayOfMonth));
    }

    private void finishDateSelection(int year, int month, int dayOfMonth) {
        String candidateDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);

        Set<String> holidays = holidaysByYear.get(year);
        if (holidays != null && holidays.contains(candidateDate)) {
            Log.d(TAG, "Date rejected - public holiday: " + candidateDate);
            Toast.makeText(this, R.string.error_public_holiday, Toast.LENGTH_LONG).show();
            return;
        }

        selectedDate = candidateDate;
        tvSelectedDate.setText(String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year));
        resetTimeSelection();
        Log.d(TAG, "Date selected: " + selectedDate);
    }

    // ---------- Free times ----------

    /**
     * Loads the 15-minute blocks already reserved on the chosen date, works
     * out which start times are free for the chosen duration and location,
     * and shows them in a list for the client to pick from.
     */
    private void showFreeTimes() {
        if (selectedDate == null) {
            Toast.makeText(this, R.string.error_pick_date_first, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!NetworkStatusHelper.isOnline(this)) {
            Toast.makeText(this, R.string.error_offline_booking, Toast.LENGTH_LONG).show();
            return;
        }

        boolean isInPerson = spinnerConsultationType.getSelectedItemPosition() == 1;
        final String locationKey = isInPerson ? spinnerLocation.getSelectedItem().toString() : "online";
        final int duration = currentDuration();
        final String date = selectedDate;

        setLoading(true);
        FirebaseRefs.bookedSlotsCollection().whereEqualTo("date", date).get()
                .addOnSuccessListener(snapshot -> {
                    setLoading(false);
                    if (isFinishing() || isDestroyed()) return;

                    Set<String> taken = new HashSet<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String slotLocation = doc.getString("location");
                        String slotKey = slotLocation != null ? slotLocation : "online";
                        String slotTime = doc.getString("time");
                        if (locationKey.equals(slotKey) && slotTime != null) taken.add(slotTime);
                    }

                    PracticeInfo info = currentPracticeInfo();
                    List<String> free = AvailabilityUtils.freeStartTimes(
                            info.computeOpeningMinutes(), info.computeClosingMinutes(), duration,
                            taken, info.breakRanges(), earliestStartFor(date));
                    presentFreeTimes(date, duration, free);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "Could not load booked slots", e);
                    Toast.makeText(this, R.string.error_availability_failed, Toast.LENGTH_LONG).show();
                });
    }

    /** For today, no time before now may be offered; for a future date there is no limit. */
    private int earliestStartFor(String date) {
        Calendar now = Calendar.getInstance();
        String today = String.format(Locale.US, "%04d-%02d-%02d",
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
        if (!today.equals(date)) return 0;
        return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE) + 1;
    }

    private void presentFreeTimes(String date, int duration, List<String> free) {
        String dateDisplay = date.substring(8, 10) + "/" + date.substring(5, 7) + "/" + date.substring(0, 4);
        String title = getString(R.string.dialog_free_times_title, dateDisplay);

        if (free.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(R.string.dialog_no_free_times)
                    .setPositiveButton(R.string.btn_ok, null)
                    .show();
            return;
        }

        String[] labels = new String[free.size()];
        for (int i = 0; i < free.size(); i++) {
            int start = PracticeInfo.parseMinutes(free.get(i), 0);
            labels[i] = free.get(i) + "  \u2013  " + PracticeInfo.formatMinutes(start + duration);
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(labels, (dialog, which) -> {
                    selectedTime = free.get(which);
                    tvSelectedTime.setText(selectedTime);
                    Log.d(TAG, "Time selected: " + selectedTime);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    /**
     * True if an appointment of the given duration starting at startMinutes
     * fits completely inside operating hours. Shows the reason if not.
     */
    private boolean timeFitsOperatingHours(int startMinutes, int duration) {
        PracticeInfo info = currentPracticeInfo();
        int opening = info.computeOpeningMinutes();
        int closing = info.computeClosingMinutes();

        if (startMinutes < opening) {
            Toast.makeText(this, getString(R.string.error_outside_hours,
                            PracticeInfo.formatMinutes(opening), PracticeInfo.formatMinutes(closing)),
                    Toast.LENGTH_LONG).show();
            return false;
        }
        if (startMinutes + duration > closing) {
            Toast.makeText(this, getString(R.string.error_appointment_runs_past_close,
                    duration, PracticeInfo.formatMinutes(closing)), Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    // ---------- Submission ----------

    private void attemptSubmit() {
        if (!NetworkStatusHelper.isOnline(this)) {
            Log.w(TAG, "Booking submission blocked - device offline");
            Toast.makeText(this, R.string.error_offline_booking, Toast.LENGTH_LONG).show();
            return;
        }

        if (selectedDate == null) {
            Toast.makeText(this, R.string.error_select_date, Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTime == null) {
            Toast.makeText(this, R.string.error_select_time, Toast.LENGTH_SHORT).show();
            return;
        }

        int duration = currentDuration();
        int startMinutes = PracticeInfo.parseMinutes(selectedTime, -1);
        PracticeInfo info = currentPracticeInfo();

        // Authoritative re-check using the fully loaded practice settings.
        if (startMinutes < 0 || startMinutes % BookedSlot.BLOCK_MINUTES != 0
                || !timeFitsOperatingHours(startMinutes, duration)) {
            return;
        }
        if (info.isDateBlocked(selectedDate)) {
            Toast.makeText(this, R.string.error_blocked_date_no_reason, Toast.LENGTH_LONG).show();
            return;
        }
        if (AvailabilityUtils.overlapsBreak(startMinutes, startMinutes + duration, info.breakRanges())) {
            Toast.makeText(this, R.string.error_time_in_break, Toast.LENGTH_LONG).show();
            return;
        }

        String uid = FirebaseRefs.getAuth().getUid();
        if (uid == null) return;

        String service = spinnerService.getSelectedItem().toString();
        boolean isInPerson = spinnerConsultationType.getSelectedItemPosition() == 1;
        String consultationType = isInPerson ? "in-person" : "online";
        String location = isInPerson ? spinnerLocation.getSelectedItem().toString() : null;
        String additionalInfo = etAdditionalInfo.getText().toString().trim();

        Log.i(TAG, "Booking submission attempt: " + service + " / " + consultationType + " / " + selectedDate + " " + selectedTime);

        setLoading(true);
        submitBookingTransaction(uid, service, consultationType, location, duration, additionalInfo);
    }

    /**
     * Reads every 15-minute block the appointment covers, fails if any is
     * already reserved (by this client or anyone else), then writes all
     * the blocks and the booking in the same transaction.
     */
    private void submitBookingTransaction(String uid, String service, String consultationType,
                                          String location, int duration, String additionalInfo) {

        final String date = selectedDate;
        final String time = selectedTime;

        List<String> blockTimes = BookedSlot.buildBlockTimes(time, duration);
        List<DocumentReference> slotRefs = new ArrayList<>();
        for (String blockTime : blockTimes) {
            slotRefs.add(FirebaseRefs.bookedSlotsCollection()
                    .document(BookedSlot.buildId(date, blockTime, location)));
        }
        DocumentReference newBookingRef = FirebaseRefs.bookingsCollection().document();

        FirebaseRefs.getFirestore().runTransaction(transaction -> {
            // All reads first (Firestore requires reads before writes).
            List<DocumentSnapshot> snapshots = new ArrayList<>();
            for (DocumentReference ref : slotRefs) {
                snapshots.add(transaction.get(ref));
            }
            for (DocumentSnapshot snapshot : snapshots) {
                if (snapshot.exists()) {
                    boolean ownBooking = uid.equals(snapshot.getString("clientId"));
                    throw new FirebaseFirestoreException(
                            ownBooking ? "SLOT_TAKEN_OWN" : "SLOT_TAKEN",
                            FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                }
            }

            for (int i = 0; i < slotRefs.size(); i++) {
                transaction.set(slotRefs.get(i),
                        new BookedSlot(uid, date, blockTimes.get(i), consultationType, location, duration));
            }

            transaction.set(newBookingRef,
                    new Booking(uid, service, consultationType, location, duration, date, time, additionalInfo));
            return null;
        }).addOnSuccessListener(unused -> {
            setLoading(false);
            Log.i(TAG, "Booking submitted successfully: " + newBookingRef.getId());

            NotificationHelper.showImmediateNotification(
                    this, "Booking Submitted", "Your " + service + " booking request has been received.");
            NotificationHelper.scheduleReminder(this, newBookingRef.getId(), date, service);

            goToConfirmation(service, consultationType, location, duration, additionalInfo);
        }).addOnFailureListener(e -> {
            setLoading(false);
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.contains("SLOT_TAKEN_OWN")) {
                Log.w(TAG, "Booking rejected - overlaps the client's own booking");
                Toast.makeText(this, R.string.error_own_overlap, Toast.LENGTH_LONG).show();
            } else if (message.contains("SLOT_TAKEN")) {
                Log.w(TAG, "Booking rejected - time already reserved by another client");
                Toast.makeText(this, R.string.error_slot_taken, Toast.LENGTH_LONG).show();
            } else if (!NetworkStatusHelper.isOnline(this)) {
                Log.w(TAG, "Booking failed - connectivity lost mid-submission");
                Toast.makeText(this, R.string.error_offline_booking, Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG, "Booking transaction failed", e);
                Toast.makeText(this, getString(R.string.error_booking_failed, message), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToConfirmation(String service, String consultationType, String location,
                                  int duration, String additionalInfo) {
        Intent intent = new Intent(this, BookingConfirmationActivity.class);
        intent.putExtra("service", service);
        intent.putExtra("consultationType", consultationType);
        intent.putExtra("location", location);
        intent.putExtra("duration", duration);
        intent.putExtra("date", selectedDate);
        intent.putExtra("time", selectedTime);
        intent.putExtra("additionalInfo", additionalInfo);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmitBooking.setEnabled(!loading);
    }
}