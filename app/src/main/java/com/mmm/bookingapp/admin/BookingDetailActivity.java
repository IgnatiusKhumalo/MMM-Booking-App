package com.mmm.bookingapp.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mmm.bookingapp.R;
import com.mmm.bookingapp.models.Booking;
import com.mmm.bookingapp.utils.FirebaseRefs;

/**
 * Screen 19 - Booking Detail Screen (Admin).
 * Confirm / Decline / Mark as Paid / Mark as Completed, per the Blueprint.
 * Decline reuses the same underlying mechanism as a client cancellation -
 * status set to "cancelled" and the matching bookedSlots entry freed -
 * since both represent the same real-world outcome (the slot becomes
 * available again).
 */
public class BookingDetailActivity extends AppCompatActivity {

    private String bookingId;
    private Booking currentBooking;

    private TextView tvStatusBadge, tvClientName, tvClientContact, tvServiceValue,
            tvTypeLocationValue, tvDateTimeValue, tvAdditionalInfoValue;
    private Switch switchPaid;
    private Button btnConfirmBooking, btnDeclineBooking, btnMarkCompleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        bookingId = getIntent().getStringExtra("bookingId");

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvClientName = findViewById(R.id.tvClientName);
        tvClientContact = findViewById(R.id.tvClientContact);
        tvServiceValue = findViewById(R.id.tvServiceValue);
        tvTypeLocationValue = findViewById(R.id.tvTypeLocationValue);
        tvDateTimeValue = findViewById(R.id.tvDateTimeValue);
        tvAdditionalInfoValue = findViewById(R.id.tvAdditionalInfoValue);
        switchPaid = findViewById(R.id.switchPaid);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        btnDeclineBooking = findViewById(R.id.btnDeclineBooking);
        btnMarkCompleted = findViewById(R.id.btnMarkCompleted);

        btnConfirmBooking.setOnClickListener(v -> updateStatus("confirmed", R.string.booking_confirmed_message));
        btnDeclineBooking.setOnClickListener(v -> declineBooking());
        btnMarkCompleted.setOnClickListener(v -> updateStatus("completed", R.string.booking_completed_message));

        loadBooking();
    }

    private void loadBooking() {
        if (bookingId == null) return;

        FirebaseRefs.bookingsCollection().document(bookingId).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) return;
                    currentBooking = document.toObject(Booking.class);
                    if (currentBooking != null) currentBooking.setBookingId(bookingId);
                    displayBooking();
                    loadClientInfo();
                });
    }

    private void loadClientInfo() {
        if (currentBooking == null || currentBooking.getClientId() == null) return;

        FirebaseRefs.usersCollection().document(currentBooking.getClientId()).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) return;
                    tvClientName.setText(document.getString("name"));
                    String phone = document.getString("phone");
                    String email = document.getString("email");
                    tvClientContact.setText(phone + "  ·  " + email);
                });
    }

    private void displayBooking() {
        if (currentBooking == null) return;

        applyStatusBadge(currentBooking.getStatus());

        tvServiceValue.setText(currentBooking.getService());

        String locationText = "in-person".equals(currentBooking.getConsultationType())
                ? currentBooking.getLocation() : "Online";
        tvTypeLocationValue.setText(
                ("in-person".equals(currentBooking.getConsultationType()) ? "In-Person" : "Online")
                        + "  ·  " + locationText);

        tvDateTimeValue.setText(currentBooking.getDate() + "  ·  " + currentBooking.getTime()
                + "  (" + currentBooking.getDuration() + " min)");

        String info = currentBooking.getAdditionalInfo();
        tvAdditionalInfoValue.setText((info == null || info.isEmpty())
                ? getString(R.string.no_additional_info) : info);

        switchPaid.setOnCheckedChangeListener(null);
        switchPaid.setChecked(currentBooking.isPaid());
        switchPaid.setOnCheckedChangeListener((buttonView, isChecked) -> updatePaidStatus(isChecked));

        boolean isPending = "pending".equals(currentBooking.getStatus());
        boolean isConfirmed = "confirmed".equals(currentBooking.getStatus());

        btnConfirmBooking.setVisibility(isPending ? View.VISIBLE : View.GONE);
        btnDeclineBooking.setVisibility(isPending ? View.VISIBLE : View.GONE);
        btnMarkCompleted.setVisibility(isConfirmed ? View.VISIBLE : View.GONE);
    }

    private void applyStatusBadge(String status) {
        int colorRes;
        String label;
        switch (status) {
            case "confirmed": colorRes = R.color.color_status_confirmed; label = getString(R.string.status_confirmed); break;
            case "cancelled": colorRes = R.color.color_error; label = getString(R.string.status_cancelled); break;
            case "completed": colorRes = R.color.color_status_completed; label = getString(R.string.status_completed); break;
            default: colorRes = R.color.color_status_pending; label = getString(R.string.status_pending);
        }
        tvStatusBadge.setText(label);
        tvStatusBadge.setBackgroundColor(getColor(colorRes));
    }

    private void updateStatus(String newStatus, int successMessageRes) {
        FirebaseRefs.bookingsCollection().document(bookingId)
                .update("status", newStatus)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, successMessageRes, Toast.LENGTH_SHORT).show();
                    loadBooking();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_action_failed, e.getMessage()), Toast.LENGTH_LONG).show());
    }

    private void declineBooking() {
        FirebaseRefs.bookingsCollection().document(bookingId)
                .update("status", "cancelled")
                .addOnSuccessListener(unused -> {
                    if (currentBooking != null) {
                        String slotLocationPart = (currentBooking.getLocation() != null) ? currentBooking.getLocation() : "online";
                        String slotId = currentBooking.getDate() + "_" + currentBooking.getTime() + "_" + slotLocationPart;
                        FirebaseRefs.bookedSlotsCollection().document(slotId).delete();
                    }
                    Toast.makeText(this, R.string.booking_declined_message, Toast.LENGTH_SHORT).show();
                    loadBooking();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_action_failed, e.getMessage()), Toast.LENGTH_LONG).show());
    }

    private void updatePaidStatus(boolean paid) {
        FirebaseRefs.bookingsCollection().document(bookingId)
                .update("paid", paid)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, R.string.paid_status_updated, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_action_failed, e.getMessage()), Toast.LENGTH_LONG).show());
    }
}