package com.mmm.bookingapp.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mmm.bookingapp.R;
import com.mmm.bookingapp.auth.WelcomeActivity;
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.NetworkStatusHelper;

/**
 * Screen 6 - Home Screen (client root screen).
 * Root screen per the Blueprint's navigation rule - no back arrow; the
 * system back button/gesture exits the app from here.
 *
 * Redesigned visually (header with pill Logout, large primary Book
 * Appointment button, 2x2 card grid for Services/My Bookings/Contact/
 * Settings) - navigation destinations and logic are UNCHANGED from the
 * previous version: the four cards route to the exact same four
 * Activities the previous outlined buttons did, and each destination
 * screen keeps its own back arrow exactly as before. No bottom
 * navigation bar was added, per the documented decision to avoid
 * duplicating navigation across every screen and risking accidental
 * data loss on form screens.
 */
public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        NetworkStatusHelper.observeConnectivity(this, findViewById(R.id.tvOfflineBanner));

        tvWelcome = findViewById(R.id.tvWelcome);
        loadWelcomeMessage();

        Button btnBookAppointment = findViewById(R.id.btnBookAppointment);
        View cardServices = findViewById(R.id.btnServices);
        View cardMyBookings = findViewById(R.id.btnMyBookings);
        View cardContact = findViewById(R.id.btnContact);
        View cardSettings = findViewById(R.id.btnSettings);
        Button btnLogoutHome = findViewById(R.id.btnLogoutHome);

        btnBookAppointment.setOnClickListener(v ->
                startActivity(new Intent(this, BookAppointmentActivity.class)));

        cardServices.setOnClickListener(v ->
                startActivity(new Intent(this, ServicesActivity.class)));

        cardMyBookings.setOnClickListener(v ->
                startActivity(new Intent(this, MyBookingsActivity.class)));

        cardContact.setOnClickListener(v ->
                startActivity(new Intent(this, ContactActivity.class)));

        cardSettings.setOnClickListener(v ->
                startActivity(new Intent(this, com.mmm.bookingapp.settings.SettingsActivity.class)));

        btnLogoutHome.setOnClickListener(v -> logout());
    }

    private void loadWelcomeMessage() {
        String uid = FirebaseRefs.getAuth().getUid();
        if (uid == null) return;

        FirebaseRefs.usersCollection().document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        if (name != null && !name.isEmpty()) {
                            String firstName = name.split(" ")[0];
                            tvWelcome.setText(getString(R.string.home_welcome_default) + ", " + firstName + "!");
                        }
                    }
                });
    }

    private void logout() {
        FirebaseRefs.getAuth().signOut();
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}