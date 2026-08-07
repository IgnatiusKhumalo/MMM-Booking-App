package com.mmm.bookingapp.client;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.mmm.bookingapp.R;
import com.mmm.bookingapp.models.Booking;
import com.mmm.bookingapp.models.BookedSlot;
import com.mmm.bookingapp.models.PracticeInfo;
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.NetworkStatusHelper;
import com.mmm.bookingapp.utils.NotificationHelper;

import java.util.Calendar;
import java.util.Locale;

/**
 * Screen 8 - Book Appointment Screen.
 * The core transaction of the app. Implements:
 * 1. Consultation Type -> conditional Location dropdown (Blueprint Section 3)
 * 2. Date/time enforcement against practiceInfo's operating days/hours
 * 3. Prevention of the CLIENT'S OWN overlapping bookings (pre-check query)
 * 4. Prevention of DOUBLE-BOOKING across different clients, via a Firestore
 *    transaction against the anonymous bookedSlots collection - this is the
 *    exact mechanism documented as the loophole fix in the Blueprint,
 *    Section 6: it avoids exposing any client's personal booking data to
 *    another client while still preventing two people booking the same slot.
 * 5. On success: an immediate local "booking submitted" notification, and
 *    a scheduled local reminder for the evening before the appointment -
 *    both entirely client-side, per the documented notification limitation
 *    (see NotificationHelper for the full explanation of why admin-action
 *    notifications are NOT implemented here).
 * 6. A live offline banner (Innovation Feature) - Firestore's built-in
 *    offline persistence means most app functions (viewing bookings,
 *    Contact, Settings) keep working offline. Booking SUBMISSION is the
 *    one deliberate exception: it uses a Firestore transaction to check
 *    bookedSlots and prevent double-booking, and Firestore transactions
 *    require live connectivity by design - they cannot be queued offline,
 *    since their entire guarantee depends on reading the server's current
 *    state before committing. This is a genuine Firestore constraint, not
 *    a bug - submission is blocked with a clear explanation while offline,
 *    rather than allowed to fail with a raw network error.
 */
public class BookAppointmentActivity extends AppCompatActivity {

    private Spinner spinnerService, spinnerConsultationType, spinnerLocation, spinnerDuration;
    private View layoutLocation;
    private TextView tvOnlineNote, tvSelectedDate, tvSelectedTime;
    private EditText etAdditionalInfo;
    private Button btnSubmitBooking;
    private ProgressBar progressBar;

    private String selectedDate = null;   // "yyyy-MM-dd"
    private String selectedTime = null;   // "HH:mm"
    private final int[] durationValues = {30, 45, 60};

    private PracticeInfo practiceInfo; // loaded once, used for hours validation

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
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        etAdditionalInfo = findViewById(R.id.etAdditionalInfo);
        btnSubmitBooking = findViewById(R.id.btnSubmitBooking);
        progressBar = findViewById(R.id.progressBar);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        setupSpinners();
        loadPracticeInfo();

        tvSelectedDate.setOnClickListener(v -> showDatePicker());
        tvSelectedTime.setOnClickListener(v -> showTimePicker());
        btnSubmitBooking.setOnClickListener(v -> attemptSubmit());
    }

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

        // Consultation Type drives the conditional Location dropdown - the
        // exact behaviour agreed and locked into the Blueprint.
        spinnerConsultationType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                boolean isInPerson = position == 1; // 0 = Online, 1 = In-Person
                layoutLocation.setVisibility(isInPerson ? View.VISIBLE : View.GONE);
                tvOnlineNote.setVisibility(isInPerson ? View.GONE : View.VISIBLE);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void loadPracticeInfo() {
        FirebaseRefs.practiceInfoDoc().get()
                .addOnSuccessListener(document -> {
                    PracticeInfo info = document.exists() ? document.toObject(PracticeInfo.class) : null;
                    practiceInfo = (info != null) ? info : PracticeInfo.getFallback();
                })
                .addOnFailureListener(e -> practiceInfo = PracticeInfo.getFallback());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, dayOfMonth);
            int dayOfWeek = picked.get(Calendar.DAY_OF_WEEK);

            // Enforce Monday-Friday only, per the Blueprint's core business rules.
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                Toast.makeText(this, R.string.error_weekend_date, Toast.LENGTH_LONG).show();
                return;
            }

            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            tvSelectedDate.setText(String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {

            // Enforce operating hours from practiceInfo (falls back to 08:00-18:00
            // if that document hasn't loaded yet - defensive, per the documented gap fix).
            String startStr = (practiceInfo != null && practiceInfo.getOperatingHours() != null)
                    ? practiceInfo.getOperatingHours().getStart() : "08:00";
            String endStr = (practiceInfo != null && practiceInfo.getOperatingHours() != null)
                    ? practiceInfo.getOperatingHours().getEnd() : "18:00";

            int startHour = Integer.parseInt(startStr.split(":")[0]);
            int endHour = Integer.parseInt(endStr.split(":")[0]);

            if (hourOfDay < startHour || hourOfDay >= endHour) {
                Toast.makeText(this, getString(R.string.error_outside_hours, startStr, endStr), Toast.LENGTH_LONG).show();
                return;
            }

            selectedTime = String.format(Locale.US, "%02d:%02d", hourOfDay, minute);
            tvSelectedTime.setText(selectedTime);
        }, calendar.get(Calendar.HOUR_OF_DAY), 0, true);

        dialog.show();
    }

    private void attemptSubmit() {
        // Booking submission requires connectivity - see class-level comment
        // for why (Firestore transactions cannot run offline). Checked
        // first, before any field validation, so the user gets this
        // specific explanation rather than a confusing network error
        // after filling in the whole form.
        if (!NetworkStatusHelper.isOnline(this)) {
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

        String uid = FirebaseRefs.getAuth().getUid();
        if (uid == null) return;

        String service = spinnerService.getSelectedItem().toString();
        boolean isInPerson = spinnerConsultationType.getSelectedItemPosition() == 1;
        String consultationType = isInPerson ? "in-person" : "online";
        String location = isInPerson ? spinnerLocation.getSelectedItem().toString() : null;
        int duration = durationValues[spinnerDuration.getSelectedItemPosition()];
        String additionalInfo = etAdditionalInfo.getText().toString().trim();

        setLoading(true);
        checkOwnOverlap(uid, service, consultationType, location, duration, additionalInfo);
    }

    /**
     * Pre-check: does the client already have a Pending/Confirmed booking
     * that overlaps this date/time, using duration to compute the interval.
     * This is a client-side pre-check (not itself transactional) - a
     * documented, reasonable simplification, since it only protects the
     * client from booking a conflict with THEMSELVES. The cross-client
     * double-booking case below is the one that genuinely needs a
     * transaction, and that one is implemented properly.
     */
    private void checkOwnOverlap(String uid, String service, String consultationType,
                                 String location, int duration, String additionalInfo) {
        FirebaseRefs.bookingsCollection()
                .whereEqualTo("clientId", uid)
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int newStart = toMinutes(selectedTime);
                    int newEnd = newStart + duration;

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        String status = doc.getString("status");
                        if (!"pending".equals(status) && !"confirmed".equals(status)) continue;

                        String existingTime = doc.getString("time");
                        Long existingDuration = doc.getLong("duration");
                        if (existingTime == null || existingDuration == null) continue;

                        int existingStart = toMinutes(existingTime);
                        int existingEnd = existingStart + existingDuration.intValue();

                        boolean overlaps = newStart < existingEnd && existingStart < newEnd;
                        if (overlaps) {
                            setLoading(false);
                            Toast.makeText(this, R.string.error_own_overlap, Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    // No conflict with the client's own bookings - proceed to the
                    // transactional cross-client check.
                    submitBookingTransaction(uid, service, consultationType, location, duration, additionalInfo);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, getString(R.string.error_booking_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    private int toMinutes(String hhmm) {
        String[] parts = hhmm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    /**
     * The real double-booking prevention mechanism. Builds a deterministic
     * slotId from date+time+location(or "online"), and uses a Firestore
     * transaction to atomically check whether that exact slot is already
     * taken, and if not, write BOTH the anonymous bookedSlots marker AND
     * the client's private bookings document together. Because both writes
     * happen inside one transaction, there is no window where two clients
     * could both pass the "slot is free" check for the same slot - this is
     * exactly the mechanism documented in the Blueprint's Section 6 fix.
     * Requires connectivity - already checked in attemptSubmit() before
     * reaching here.
     */
    private void submitBookingTransaction(String uid, String service, String consultationType,
                                          String location, int duration, String additionalInfo) {

        String slotLocationPart = (location != null) ? location : "online";
        String slotId = selectedDate + "_" + selectedTime + "_" + slotLocationPart;

        DocumentReference slotRef = FirebaseRefs.bookedSlotsCollection().document(slotId);
        DocumentReference newBookingRef = FirebaseRefs.bookingsCollection().document();

        FirebaseRefs.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot slotSnapshot = transaction.get(slotRef);
            if (slotSnapshot.exists()) {
                // Someone else already holds this exact slot - abort the
                // whole transaction, nothing gets written.
                throw new RuntimeException("SLOT_TAKEN");
            }

            BookedSlot bookedSlot = new BookedSlot(selectedDate, selectedTime, consultationType, location, duration);
            transaction.set(slotRef, bookedSlot);

            Booking booking = new Booking(uid, service, consultationType, location, duration, selectedDate, selectedTime, additionalInfo);
            transaction.set(newBookingRef, booking);

            return null;
        }).addOnSuccessListener(unused -> {
            setLoading(false);

            NotificationHelper.showImmediateNotification(
                    this, "Booking Submitted", "Your " + service + " booking request has been received.");
            NotificationHelper.scheduleReminder(this, newBookingRef.getId(), selectedDate, service);

            goToConfirmation(service, consultationType, location, duration, additionalInfo);
        }).addOnFailureListener(e -> {
            setLoading(false);
            if ("SLOT_TAKEN".equals(e.getMessage())) {
                Toast.makeText(this, R.string.error_slot_taken, Toast.LENGTH_LONG).show();
            } else if (!NetworkStatusHelper.isOnline(this)) {
                // Connectivity dropped mid-submission (e.g. between the
                // initial check and now) - same honest explanation rather
                // than the raw Firestore error message.
                Toast.makeText(this, R.string.error_offline_booking, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, getString(R.string.error_booking_failed, e.getMessage()), Toast.LENGTH_LONG).show();
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