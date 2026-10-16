package com.mmm.bookingapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.mmm.bookingapp.admin.AdminDashboardActivity;
import com.mmm.bookingapp.auth.WelcomeActivity;
import com.mmm.bookingapp.client.HomeActivity;
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.NotificationHelper;
import com.mmm.bookingapp.utils.SessionCache;

/**
 * Screen 1 - Splash Screen.
 * The app's real launcher. Creates the notification channel (required
 * once, before any notification can be shown), shows the logo briefly
 * while Firebase initialises, then checks whether a user is already
 * logged in:
 * - If yes, skips Welcome/Login and routes straight to the correct home
 *   screen by role, so a returning user never sees Welcome again.
 * - If the role cannot be fetched (for example offline with no cached
 *   copy), the role cached on this device at the last successful login
 *   is used instead, so a logged-in user is not sent back to Welcome.
 * - If no user is logged in, routes to WelcomeActivity.
 */
public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationHelper.createNotificationChannel(this);

        new Handler(Looper.getMainLooper()).postDelayed(this::checkLoginStateAndRoute, SPLASH_DELAY_MS);
    }

    private void checkLoginStateAndRoute() {
        FirebaseUser currentUser = FirebaseRefs.getAuth().getCurrentUser();

        if (currentUser == null) {
            goTo(WelcomeActivity.class);
            return;
        }

        // Force-refresh verification status before trusting it, same
        // approach used in LoginActivity.
        currentUser.reload().addOnCompleteListener(task -> {
            if (!currentUser.isEmailVerified()) {
                // Logged in but unverified: unverified accounts cannot
                // access the app, so send back to Welcome.
                FirebaseRefs.getAuth().signOut();
                goTo(WelcomeActivity.class);
                return;
            }

            String uid = currentUser.getUid();
            FirebaseRefs.usersCollection().document(uid).get()
                    .addOnSuccessListener(document -> {
                        if (!document.exists()) {
                            FirebaseRefs.getAuth().signOut();
                            goTo(WelcomeActivity.class);
                            return;
                        }
                        String role = document.getString("role");
                        SessionCache.saveRole(this, uid, role);
                        routeByRole(role);
                    })
                    .addOnFailureListener(e -> {
                        // Role could not be fetched. Use the role remembered from the
                        // last successful login; only fall back to Welcome if none exists.
                        String cachedRole = SessionCache.getRole(this, uid);
                        if (cachedRole != null) {
                            routeByRole(cachedRole);
                        } else {
                            goTo(WelcomeActivity.class);
                        }
                    });
        });
    }

    private void routeByRole(String role) {
        if ("admin".equals(role)) {
            goTo(AdminDashboardActivity.class);
        } else {
            goTo(HomeActivity.class);
        }
    }

    private void goTo(Class<?> activityClass) {
        Intent intent = new Intent(MainActivity.this, activityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}