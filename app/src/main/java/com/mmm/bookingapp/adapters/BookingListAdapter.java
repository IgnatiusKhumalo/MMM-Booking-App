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
import com.mmm.bookingapp.utils.StatusUtils;

import java.util.List;

/**
 * RecyclerView adapter shared by My Bookings (Screen 10) and the Admin
 * Dashboard (Screen 18), per the Project Structure document.
 *
 * Client mode (adminView = false): each row shows a Cancel button on
 * pending and confirmed bookings.
 * Admin mode (adminView = true): the row is tappable to open Booking
 * Detail and shows no Cancel button - admin actions live on that screen.
 *
 * The status badge text and colour come from StatusUtils, which treats a
 * missing status as "pending" so a row can never crash the list.
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

        holder.tvStatusBadge.setText(StatusUtils.badgeLabelRes(booking.getStatus()));
        holder.tvStatusBadge.setBackgroundColor(context.getColor(StatusUtils.badgeColorRes(booking.getStatus())));

        boolean showCancel = !adminView && StatusUtils.isActive(booking.getStatus());
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