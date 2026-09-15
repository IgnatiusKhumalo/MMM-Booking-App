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

import com.google.firebase.firestore.ListenerRegistration;
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
 *
 * REAL-TIME UPDATE (new): bookings are now loaded via addSnapshotListener
 * instead of a one-time .get() call in onResume(). If an admin confirms
 * or declines this client's booking while this screen is open, the
 * status badge now updates automatically, with no manual refresh needed.
 * The listener is registered once in onCreate() and removed in
 * onDestroy() to avoid a leak.
 *
 * Offline behaviour is unchanged in substance: the listener still serves
 * cached data when offline (Firestore's own built-in behaviour), and the
 * offline banner is now driven by NetworkStatusHelper.observeConnectivity
 * (same live-updating pattern already used on Home and Book Appointment)
 * rather than a one-time check.
 */
public class MyBookingsActivity extends AppCompatActivity implements BookingListAdapter.OnBookingActionListener {

    private RecyclerView recyclerBookings;
    private View tvEmptyState;
    private TextView tvOfflineBanner;
    private TextView tvBookingHistorySummary;
    private final List<Booking> bookingList = new ArrayList<>();
    private BookingListAdapter adapter;

    private ListenerRegistration bookingsListenerRegistration;

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

        NetworkStatusHelper.observeConnectivity(this, tvOfflineBanner);

        attachBookingsListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookingsListenerRegistration != null) {
            bookingsListenerRegistration.remove();
        }
    }

    private void attachBookingsListener() {
        String uid = FirebaseRefs.getAuth().getUid();
        if (uid == null) return;

        bookingsListenerRegistration = FirebaseRefs.bookingsCollection()
                .whereEqualTo("clientId", uid)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        if (!NetworkStatusHelper.isOnline(this)) {
                            bookingList.clear();
                            adapter.notifyDataSetChanged();
                            tvEmptyState.setVisibility(View.VISIBLE);
                            recyclerBookings.setVisibility(View.GONE);
                            tvBookingHistorySummary.setVisibility(View.GONE);
                        } else {
                            Toast.makeText(this, getString(R.string.error_cancel_failed, error.getMessage()), Toast.LENGTH_LONG).show();
                        }
                        return;
                    }
                    if (querySnapshot == null) return;

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
                });
    }

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
                    // No manual reload needed - the real-time listener above
                    // picks up this change automatically.
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_cancel_failed, e.getMessage()), Toast.LENGTH_LONG).show()
                );
    }
}