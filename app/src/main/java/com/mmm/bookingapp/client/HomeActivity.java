package com.mmm.bookingapp.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mmm.bookingapp.R;
import com.mmm.bookingapp.auth.WelcomeActivity;
import com.mmm.bookingapp.utils.DialogUtils;
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.NetworkStatusHelper;

/**
 * Screen 6 - Home Screen (client root screen).
 *
 * The hub for a logged-in client: a Book Appointment button plus cards
 * for Services, My Bookings, Contact and Settings, and a live offline
 * banner. It greets the client by first name and shows a brief "Logged in
 * successfully" toast when reached from a fresh login (the
 * "showLoginSuccess" intent extra).
 *
 * As a root screen it has no back arrow. The system back button uses
 * double-press-to-exit: the first press shows a toast, a second press
 * within 2 seconds exits the app. This never signs the user out - the
 * login session stays intact and the user is only logged out through the
 * Logout button, which asks for confirmation first.
 */
public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private long backPressedTime = 0;
    private static final long BACK_PRESS_EXIT_INTERVAL = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (getIntent().getBooleanExtra("showLoginSuccess", false)) {
            Toast.makeText(this, R.string.login_success_message, Toast.LENGTH_SHORT).show();
        }

        NetworkStatusHelper.observeConnectivity(this, findViewById(R.id.tvOfflineBanner));

        tvWelcome = findViewById(R.id.tvWelcome);
        loadWelcomeMessage();

        Button btnBookAppointment = findViewById(R.id.btnBookAppointment);
        View cardServices = findViewById(R.id.btnServices);
        View cardMyBookings = findViewById(R.id.btnMyBookings);
        View cardContact = findViewById(R.id.btnContact);
        View cardSettings = findViewById(R.id.btnSettings);
        View btnLogoutHome = findViewById(R.id.btnLogoutHome);
        View layoutFooterSupport = findViewById(R.id.layoutFooterSupport);

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

        btnLogoutHome.setOnClickListener(v ->
                DialogUtils.showLogoutConfirmation(this, this::logout));

        layoutFooterSupport.setOnClickListener(v ->
                startActivity(new Intent(this, ContactActivity.class)));
    }

    /**
     * Double-press-to-exit. Does not touch the Firebase session: the user
     * stays logged in and simply leaves the app on the second press.
     */
    @Override
    public void onBackPressed() {
        if (backPressedTime + BACK_PRESS_EXIT_INTERVAL > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, R.string.press_back_again_to_exit, Toast.LENGTH_SHORT).show();
        }
        backPressedTime = System.currentTimeMillis();
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