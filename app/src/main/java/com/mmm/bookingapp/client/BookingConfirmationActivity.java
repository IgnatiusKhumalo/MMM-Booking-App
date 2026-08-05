package com.mmm.bookingapp.client;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mmm.bookingapp.R;

/**
 * Screen 9 - Booking Confirmation Screen.
 * Displays a summary of exactly what was submitted (Blueprint, Screen 9),
 * with status shown as "Pending", then returns to Home.
 */
public class BookingConfirmationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirmation);

        TextView tvService = findViewById(R.id.tvSummaryService);
        TextView tvType = findViewById(R.id.tvSummaryType);
        TextView tvLocation = findViewById(R.id.tvSummaryLocation);
        TextView tvDuration = findViewById(R.id.tvSummaryDuration);
        TextView tvDate = findViewById(R.id.tvSummaryDate);
        TextView tvTime = findViewById(R.id.tvSummaryTime);
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

        btnBackToHome.setOnClickListener(v -> {
            Intent homeIntent = new Intent(this, HomeActivity.class);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
            finish();
        });
    }
}