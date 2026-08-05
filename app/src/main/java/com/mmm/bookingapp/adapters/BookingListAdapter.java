package com.mmm.bookingapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mmm.bookingapp.R;
import com.mmm.bookingapp.models.Booking;

import java.util.List;

/**
 * RecyclerView adapter shared by My Bookings (Screen 10) and Admin Dashboard
 * (Screen 18), per the Project Structure document. In client mode
 * (adminView = false), each row shows a Cancel button on pending/confirmed
 * bookings. In admin mode (adminView = true, wired up in a later batch),
 * the row instead becomes tappable to open Booking Detail, and no Cancel
 * button is shown - admin actions live on that separate screen.
 */
public class BookingListAdapter extends RecyclerView.Adapter<BookingListAdapter.BookingViewHolder> {

    public interface OnBookingActionListener {
        void onCancelClick(Booking booking);
        default void onItemClick(Booking booking) {}
    }

    private final List<Booking> bookings;
    private final boolean adminView;
    private final OnBookingActionListener listener;

    public BookingListAdapter(List<Booking> bookings, boolean adminView, OnBookingActionListener listener) {
        this.bookings = bookings;
        this.adminView = adminView;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking_card, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        android.content.Context context = holder.itemView.getContext();

        holder.tvServiceName.setText(booking.getService());

        String locationText = "in-person".equals(booking.getConsultationType())
                ? booking.getLocation()
                : "Online";
        String details = booking.getDate() + "  ·  " + booking.getTime()
                + "  ·  " + locationText + "  ·  " + booking.getDuration() + " min";
        holder.tvBookingDetails.setText(details);

        applyStatusBadge(holder, booking.getStatus(), context);

        boolean showCancel = !adminView
                && ("pending".equals(booking.getStatus()) || "confirmed".equals(booking.getStatus()));
        holder.btnCancelBooking.setVisibility(showCancel ? View.VISIBLE : View.GONE);
        holder.btnCancelBooking.setOnClickListener(v -> {
            if (listener != null) listener.onCancelClick(booking);
        });

        if (adminView) {
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(booking);
            });
        } else {
            holder.itemView.setOnClickListener(null);
        }
    }

    private void applyStatusBadge(BookingViewHolder holder, String status, android.content.Context context) {
        int colorRes;
        String label;
        switch (status) {
            case "confirmed":
                colorRes = R.color.color_status_confirmed;
                label = context.getString(R.string.status_confirmed);
                break;
            case "cancelled":
                colorRes = R.color.color_error;
                label = context.getString(R.string.status_cancelled);
                break;
            case "completed":
                colorRes = R.color.color_status_completed;
                label = context.getString(R.string.status_completed);
                break;
            default:
                colorRes = R.color.color_status_pending;
                label = context.getString(R.string.status_pending);
        }
        holder.tvStatusBadge.setText(label);
        holder.tvStatusBadge.setBackgroundColor(context.getColor(colorRes));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvServiceName, tvBookingDetails, tvStatusBadge;
        Button btnCancelBooking;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvServiceName = itemView.findViewById(R.id.tvServiceName);
            tvBookingDetails = itemView.findViewById(R.id.tvBookingDetails);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            btnCancelBooking = itemView.findViewById(R.id.btnCancelBooking);
        }
    }
}