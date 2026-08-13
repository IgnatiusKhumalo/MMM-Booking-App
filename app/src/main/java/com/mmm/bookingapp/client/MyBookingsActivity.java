package com.mmm.bookingapp.client;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mmm.bookingapp.R;
import com.mmm.bookingapp.adapters.BookingListAdapter;
import com.mmm.bookingapp.models.Booking;
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.NetworkStatusHelper;
import com.mmm.bookingapp.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Screen 10 - My Bookings Screen.
 * Loads the logged-in client's own bookings (Firestore Security Rules
 * already enforce that a client can only read their own bookings, per
 * the Blueprint's Section 5). Cancel requires confirming the custom
 * permanent-action dialog, and also cancels that booking's scheduled
 * reminder notification.
 *
 * Offline behaviour, precisely: Firestore's offline cache only holds data
 * already loaded at least once while online. A first-ever visit to this
 * screen while offline shows the empty state, not an error.
 *
 * Booking history summary (NEW): a simple client-facing count of bookings
 * made this calendar year, computed entirely from the booking list already
 * loaded for this screen - no new Firestore read required. Chosen as a
 * replacement for session-time/login-time tracking, which would have
 * required new tracking infrastructure and raised privacy considerations
 * beyond what this app's Privacy Policy currently covers.
 */
public class MyBookingsActivity extends AppCompatActivity implements BookingListAdapter.OnBookingActionListener {

    private RecyclerView recyclerBookings;
    private View tvEmptyState;
    private TextView tvOfflineBanner;
    private TextView tvBookingHistorySummary;
    private final List<Booking> bookingList = new ArrayList<>();
    private BookingListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        recyclerBookings = findViewById(R.id.recyclerBookings);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvOfflineBanner = findViewById(R.id.tvOfflineBanner);
        tvBookingHistorySummary = findViewById(R.id.tvBookingHistorySummary);
        recyclerBookings.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BookingListAdapter(bookingList, false, this);
        recyclerBookings.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookings();
    }

    private void loadBookings() {
        String uid = FirebaseRefs.getAuth().getUid();
        if (uid == null) return;

        boolean online = NetworkStatusHelper.isOnline(this);
        tvOfflineBanner.setVisibility(online ? View.GONE : View.VISIBLE);

        FirebaseRefs.bookingsCollection()
                .whereEqualTo("clientId", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    bookingList.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        Booking booking = doc.toObject(Booking.class);
                        booking.setBookingId(doc.getId());
                        bookingList.add(booking);
                    }
                    adapter.notifyDataSetChanged();
                    tvEmptyState.setVisibility(bookingList.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerBookings.setVisibility(bookingList.isEmpty() ? View.GONE : View.VISIBLE);
                    updateBookingHistorySummary();
                })
                .addOnFailureListener(e -> {
                    if (!NetworkStatusHelper.isOnline(this)) {
                        bookingList.clear();
                        adapter.notifyDataSetChanged();
                        tvEmptyState.setVisibility(View.VISIBLE);
                        recyclerBookings.setVisibility(View.GONE);
                        tvBookingHistorySummary.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(this, getString(R.string.error_cancel_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Counts how many of the already-loaded bookings fall within the
     * current calendar year (by comparing the "yyyy" prefix of each
     * booking's date string), and displays it as a simple summary line.
     * No new Firestore read - reuses bookingList already in memory.
     */
    private void updateBookingHistorySummary() {
        if (bookingList.isEmpty()) {
            tvBookingHistorySummary.setVisibility(View.GONE);
            return;
        }
        String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        int countThisYear = 0;
        for (Booking b : bookingList) {
            if (b.getDate() != null && b.getDate().startsWith(currentYear)) {
                countThisYear++;
            }
        }
        tvBookingHistorySummary.setText(getString(R.string.booking_history_summary, countThisYear));
        tvBookingHistorySummary.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCancelClick(Booking booking) {
        if (!NetworkStatusHelper.isOnline(this)) {
            Toast.makeText(this, R.string.error_offline_booking, Toast.LENGTH_LONG).show();
            return;
        }
        showCancelConfirmationDialog(booking);
    }

    private void showCancelConfirmationDialog(Booking booking) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cancel_confirmation, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        Button btnKeepBooking = dialogView.findViewById(R.id.btnKeepBooking);
        Button btnYesCancel = dialogView.findViewById(R.id.btnYesCancel);

        btnKeepBooking.setOnClickListener(v -> dialog.dismiss());
        btnYesCancel.setOnClickListener(v -> {
            dialog.dismiss();
            performCancellation(booking);
        });

        dialog.show();
    }

    private void performCancellation(Booking booking) {
        FirebaseRefs.bookingsCollection().document(booking.getBookingId())
                .update("status", "cancelled")
                .addOnSuccessListener(unused -> {
                    String slotLocationPart = (booking.getLocation() != null) ? booking.getLocation() : "online";
                    String slotId = booking.getDate() + "_" + booking.getTime() + "_" + slotLocationPart;

                    FirebaseRefs.bookedSlotsCollection().document(slotId).delete();
                    NotificationHelper.cancelReminder(this, booking.getBookingId());

                    Toast.makeText(this, R.string.booking_cancelled_message, Toast.LENGTH_SHORT).show();
                    loadBookings();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_cancel_failed, e.getMessage()), Toast.LENGTH_LONG).show()
                );
    }
}