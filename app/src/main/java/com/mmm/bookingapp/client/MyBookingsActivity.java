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
import com.mmm.bookingapp.utils.BookingActions;
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.NetworkStatusHelper;
import com.mmm.bookingapp.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Screen 10 - My Bookings Screen.
 *
 * Bookings are loaded through a real-time Firestore listener, so a
 * status change made by an admin appears without a manual refresh. The
 * listener is registered once in onCreate() and removed in onDestroy().
 *
 * Cancelling a booking goes through BookingActions.cancelBooking(),
 * which changes the booking status and frees its slot in a single
 * atomic transaction. Cancelling is the client's version of "delete":
 * the record is kept (for booking history) and only an admin can
 * permanently remove it.
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

    /**
     * Atomic cancel: booking status change + slot release happen together
     * inside BookingActions. The real-time listener above refreshes the
     * list automatically once the transaction commits.
     */
    private void performCancellation(Booking booking) {
        BookingActions.cancelBooking(booking.getBookingId(), new BookingActions.Callback() {
            @Override
            public void onSuccess() {
                NotificationHelper.cancelReminder(MyBookingsActivity.this, booking.getBookingId());
                Toast.makeText(MyBookingsActivity.this, R.string.booking_cancelled_message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int reason, String message) {
                if (reason == BookingActions.REASON_NOT_CANCELLABLE) {
                    Toast.makeText(MyBookingsActivity.this, R.string.error_booking_not_cancellable, Toast.LENGTH_LONG).show();
                } else if (reason == BookingActions.REASON_MISSING) {
                    Toast.makeText(MyBookingsActivity.this, R.string.error_booking_missing, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MyBookingsActivity.this,
                            getString(R.string.error_cancel_failed, message), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}