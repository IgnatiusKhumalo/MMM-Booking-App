package com.mmm.bookingapp.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mmm.bookingapp.R;

/**
 * Screen 9 - Booking Confirmation Screen.
 *
 * Shows a summary of exactly what was submitted, with the status as
 * "Pending". For In-Person bookings it also shows the clinic's full
 * address, and a "What happens next" card explains the next steps
 * (fee quotation, confirmation, free cancellation until 24 hours before).
 * From here the client can go to My Bookings or back to Home.
 */
public class BookingConfirmationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirmation);

        TextView tvService = findViewById(R.id.tvSummaryService);
        TextView tvType = findViewById(R.id.tvSummaryType);
        TextView tvLocation = findViewById(R.id.tvSummaryLocation);
        TextView tvAddress = findViewById(R.id.tvSummaryAddress);
        TextView tvDuration = findViewById(R.id.tvSummaryDuration);
        TextView tvDate = findViewById(R.id.tvSummaryDate);
        TextView tvTime = findViewById(R.id.tvSummaryTime);
        Button btnViewMyBookings = findViewById(R.id.btnViewMyBookings);
        Button btnBackToHome = findViewById(R.id.btnBackToHome);

        Intent intent = getIntent();
        String service = intent.getStringExtra("service");
        String consultationType = intent.getStringExtra("consultationType");
        String location = intent.getStringExtra("location");
        int duration = intent.getIntExtra("duration", 0);
        String date = intent.getStringExtra("date");
        String time = intent.getStringExtra("time");

        tvService.setText("Service: " + service);
        tvType.setText("Type: " + ("in-person".equals(consultationType) ? "In-Person" : "Online"));
        tvLocation.setText("Location: " + (location != null ? location : "Online"));
        tvDuration.setText("Duration: " + duration + " min");
        tvDate.setText("Date: " + date);
        tvTime.setText("Time: " + time);

        showAddressIfInPerson(tvAddress, location);

        btnViewMyBookings.setOnClickListener(v -> {
            // Home first, then My Bookings on top, so Back from My Bookings lands on Home.
            Intent homeIntent = new Intent(this, HomeActivity.class);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
            startActivity(new Intent(this, MyBookingsActivity.class));
            finish();
        });

        btnBackToHome.setOnClickListener(v -> {
            Intent homeIntent = new Intent(this, HomeActivity.class);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
            finish();
        });
    }

    /**
     * Finds the clinic address for the booked location (the location_addresses
     * array is in the same order as physical_locations). Hidden for Online.
     */
    private void showAddressIfInPerson(TextView tvAddress, String location) {
        if (location == null) {
            tvAddress.setVisibility(View.GONE);
            return;
        }
        String[] locations = getResources().getStringArray(R.array.physical_locations);
        String[] addresses = getResources().getStringArray(R.array.location_addresses);
        for (int i = 0; i < locations.length && i < addresses.length; i++) {
            if (locations[i].equals(location)) {
                tvAddress.setText(getString(R.string.label_summary_address, addresses[i]));
                tvAddress.setVisibility(View.VISIBLE);
                return;
            }
        }
        tvAddress.setVisibility(View.GONE);
    }
}