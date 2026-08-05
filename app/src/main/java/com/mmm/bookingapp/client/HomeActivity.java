package com.mmm.bookingapp.client;

import android.content.Intent;
import android.os.Bundle;
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
 * system back button/gesture exits the app from here. Welcome message is
 * personalised using the client's own name from their Firestore "users"
 * document, read once on load. Also observes live connectivity to show
 * the offline banner (Innovation Feature UI indicator).
 *
 * A direct Logout button now sits beside the welcome message, so a client
 * can sign out without navigating into Settings first.
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
        Button btnServices = findViewById(R.id.btnServices);
        Button btnMyBookings = findViewById(R.id.btnMyBookings);
        Button btnContact = findViewById(R.id.btnContact);
        Button btnSettings = findViewById(R.id.btnSettings);
        Button btnLogoutHome = findViewById(R.id.btnLogoutHome);

        btnBookAppointment.setOnClickListener(v ->
                startActivity(new Intent(this, BookAppointmentActivity.class)));

        btnServices.setOnClickListener(v ->
                startActivity(new Intent(this, ServicesActivity.class)));

        btnMyBookings.setOnClickListener(v ->
                startActivity(new Intent(this, MyBookingsActivity.class)));

        btnContact.setOnClickListener(v ->
                startActivity(new Intent(this, ContactActivity.class)));

        btnSettings.setOnClickListener(v ->
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
        // No failure handling needed here beyond leaving the default
        // "Welcome" text already set in the layout - non-critical UI text.
    }

    private void logout() {
        FirebaseRefs.getAuth().signOut();
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}