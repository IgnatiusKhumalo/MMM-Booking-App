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
import java.util.List;

/**
 * Screen 10 - My Bookings Screen.
 * Loads the logged-in client's own bookings (Firestore Security Rules
 * already enforce that a client can only read their own bookings, per
 * the Blueprint's Section 5 - this query would return nothing for anyone
 * else's data even if attempted). Cancel requires confirming the custom
 * permanent-action dialog before anything is written, and also cancels
 * that booking's scheduled reminder notification.
 *
 * Offline behaviour, precisely: Firestore's offline cache only holds data
 * that was already loaded at least once while online. If this screen has
 * never successfully loaded before on this device, opening it offline
 * will genuinely show nothing to load - not a bug, but a real limit of
 * what "offline" can mean (it preserves what was already seen, it cannot
 * retrieve data it never received). The banner below makes this explicit
 * rather than leaving an unexplained empty or failed state.
 */
public class MyBookingsActivity extends AppCompatActivity implements BookingListAdapter.OnBookingActionListener {

    private RecyclerView recyclerBookings;
    private View tvEmptyState;
    private TextView tvOfflineBanner;
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
        recyclerBookings.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BookingListAdapter(bookingList, false, this);
        recyclerBookings.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload every time the screen becomes visible, so a booking made
        // or cancelled elsewhere is reflected immediately on return.
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
                })
                .addOnFailureListener(e -> {
                    // No cache existed to fall back on (nothing was ever
                    // loaded on this device while online) - show the empty
                    // state rather than a raw error, since the offline
                    // banner above already explains why.
                    if (!NetworkStatusHelper.isOnline(this)) {
                        bookingList.clear();
                        adapter.notifyDataSetChanged();
                        tvEmptyState.setVisibility(View.VISIBLE);
                        recyclerBookings.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(this, getString(R.string.error_cancel_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                    }
                });
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

    /**
     * Sets status to "cancelled" and deletes the matching bookedSlots
     * document, freeing that slot for other clients - per the Blueprint's
     * documented cancel behaviour. The slotId is reconstructed using the
     * exact same format used at booking creation (BookAppointmentActivity),
     * so both stay in sync. Also cancels the booking's scheduled reminder,
     * so a cancelled appointment doesn't still trigger a reminder later.
     */
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