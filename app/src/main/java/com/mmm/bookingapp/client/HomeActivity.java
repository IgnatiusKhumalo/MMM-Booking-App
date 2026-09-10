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
 * Redesigned per the detailed mockup spec: minimal circular Logout icon,
 * a branding card showing the real logo edge-to-edge with no wrapper,
 * a "Next Appointment" card housing the primary Book Appointment action,
 * a 2x2 feature grid with icon+title+subtitle per card, and a tappable
 * footer support bar routing to Contact.
 *
 * "No upcoming bookings" is currently STATIC text matching the mockup -
 * this does not yet query Firestore for a real next booking. Wiring
 * that up is a straightforward follow-up if wanted, but is a functional
 * (not visual) change and was deliberately left out of this pass.
 *
 * Navigation destinations and core logic are otherwise UNCHANGED: the
 * four grid cards route to the exact same four Activities as before.
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
        View btnLogoutHome = findViewById(R.id.btnLogoutHome); // now ImageButton, typed as View
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

        btnLogoutHome.setOnClickListener(v -> logout());

        // Footer support bar - tappable, routes to the same Contact screen
        // as the Contact card, since the right-arrow implies navigation.
        layoutFooterSupport.setOnClickListener(v ->
                startActivity(new Intent(this, ContactActivity.class)));
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