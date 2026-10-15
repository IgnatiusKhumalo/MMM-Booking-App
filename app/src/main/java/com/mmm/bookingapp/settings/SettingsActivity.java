package com.mmm.bookingapp.settings;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mmm.bookingapp.R;
import com.mmm.bookingapp.auth.WelcomeActivity;
import com.mmm.bookingapp.client.ContactActivity;
import com.mmm.bookingapp.models.PracticeInfo;
import com.mmm.bookingapp.utils.DialogUtils;
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.NotificationHelper;
import com.mmm.bookingapp.utils.ThemeManager;

/**
 * Screen 12 - Settings Screen.
 *
 * Booking Reminders switch: the choice is saved on the account in
 * Firestore and cached on the device (NotificationHelper), so every
 * notification the app creates actually obeys it. Switching OFF also
 * cancels the reminders already scheduled for the user's active
 * bookings; switching back ON reschedules them.
 *
 * The switch's listener is attached only AFTER the saved value has been
 * applied, so opening this screen never fires a false "enabled" toast or
 * a needless write.
 *
 * Also provides Change Password / Change Email (Security card), a
 * confirmation dialog before logout, and a Delete My Account explainer
 * dialog shown before the email app opens.
 */
public class SettingsActivity extends AppCompatActivity {

    private EditText etFullName, etPhone;
    private TextView tvEmail, tvAppVersion;
    private Switch switchNotifications, switchDarkMode;
    private String practiceContactEmail = "info@mmmsocialworkersandassociates.co.za"; // fallback

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        tvEmail = findViewById(R.id.tvEmail);
        tvAppVersion = findViewById(R.id.tvAppVersion);
        switchNotifications = findViewById(R.id.switchNotifications);
        switchDarkMode = findViewById(R.id.switchDarkMode);

        // Show the device's cached choice straight away (works offline); the
        // account's saved value replaces it once it loads.
        switchNotifications.setChecked(NotificationHelper.areRemindersEnabled(this));
        attachNotificationsListener();

        loadUserData();
        loadPracticeEmail();
        displayAppVersion();

        switchDarkMode.setChecked(ThemeManager.isDarkModeEnabled(this));
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ThemeManager.setDarkModeEnabled(this, isChecked);
            Toast.makeText(this,
                    isChecked ? R.string.dark_mode_enabled_message : R.string.dark_mode_disabled_message,
                    Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnSaveChanges).setOnClickListener(v -> saveAccountChanges());
        findViewById(R.id.btnDeleteAccount).setOnClickListener(v ->
                DialogUtils.showDeleteAccountExplainer(this, this::launchDeleteAccountEmailApp));
        findViewById(R.id.btnChangePassword).setOnClickListener(v ->
                startActivity(new Intent(this, ChangePasswordActivity.class)));
        findViewById(R.id.btnChangeEmail).setOnClickListener(v ->
                startActivity(new Intent(this, ChangeEmailActivity.class)));
        findViewById(R.id.btnAbout).setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));
        findViewById(R.id.btnPrivacyPolicy).setOnClickListener(v ->
                startActivity(new Intent(this, PrivacyPolicyActivity.class)));
        findViewById(R.id.btnTerms).setOnClickListener(v ->
                startActivity(new Intent(this, TermsActivity.class)));
        findViewById(R.id.btnContactUs).setOnClickListener(v ->
                startActivity(new Intent(this, ContactActivity.class)));
        findViewById(R.id.btnFAQ).setOnClickListener(v ->
                startActivity(new Intent(this, FAQActivity.class)));

        findViewById(R.id.btnLogout).setOnClickListener(v ->
                DialogUtils.showLogoutConfirmation(this, this::logout));
    }

    private void loadUserData() {
        String uid = FirebaseRefs.getAuth().getUid();
        if (uid == null) return;

        FirebaseRefs.usersCollection().document(uid).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) return;
                    etFullName.setText(document.getString("name"));
                    etPhone.setText(document.getString("phone"));
                    tvEmail.setText(document.getString("email"));

                    // Apply the account's saved reminder choice WITHOUT firing the
                    // listener, refresh the device cache, then re-attach the listener.
                    Boolean notificationsEnabled = document.getBoolean("notificationsEnabled");
                    boolean remindersOn = notificationsEnabled == null || notificationsEnabled;
                    switchNotifications.setOnCheckedChangeListener(null);
                    switchNotifications.setChecked(remindersOn);
                    NotificationHelper.setRemindersEnabled(this, remindersOn);
                    attachNotificationsListener();

                    String role = document.getString("role");
                    if ("admin".equals(role)) {
                        findViewById(R.id.layoutAdminSection).setVisibility(android.view.View.VISIBLE);
                        findViewById(R.id.btnPracticeInfo).setOnClickListener(v ->
                                startActivity(new Intent(this, com.mmm.bookingapp.admin.PracticeInfoActivity.class)));
                    }
                });
    }

    private void attachNotificationsListener() {
        switchNotifications.setOnCheckedChangeListener(
                (buttonView, isChecked) -> onRemindersToggled(isChecked));
    }

    /**
     * Handles the user flipping Booking Reminders: save locally FIRST (the
     * scheduler checks it), save on the account, cancel or reschedule the
     * reminders for active bookings, then confirm with a toast.
     */
    private void onRemindersToggled(boolean enabled) {
        String uid = FirebaseRefs.getAuth().getUid();
        if (uid == null) return;

        NotificationHelper.setRemindersEnabled(this, enabled);
        FirebaseRefs.usersCollection().document(uid).update("notificationsEnabled", enabled);
        syncScheduledReminders(uid, enabled);

        Toast.makeText(this,
                enabled ? R.string.notifications_enabled_message : R.string.notifications_disabled_message,
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Cancels (switch off) or reschedules (switch on) the evening-before
     * reminder of every active - pending or confirmed - booking this user
     * owns. Finished bookings never had a live reminder worth touching.
     */
    private void syncScheduledReminders(String uid, boolean enabled) {
        FirebaseRefs.bookingsCollection()
                .whereEqualTo("clientId", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String status = doc.getString("status");
                        if (!"pending".equals(status) && !"confirmed".equals(status)) continue;

                        if (enabled) {
                            NotificationHelper.scheduleReminder(
                                    this, doc.getId(), doc.getString("date"), doc.getString("service"));
                        } else {
                            NotificationHelper.cancelReminder(this, doc.getId());
                        }
                    }
                });
    }

    private void loadPracticeEmail() {
        FirebaseRefs.practiceInfoDoc().get()
                .addOnSuccessListener(document -> {
                    PracticeInfo info = document.exists() ? document.toObject(PracticeInfo.class) : null;
                    if (info != null && info.getEmail() != null) {
                        practiceContactEmail = info.getEmail();
                    }
                });
    }

    private void displayAppVersion() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            tvAppVersion.setText(packageInfo.versionName);
        } catch (Exception e) {
            tvAppVersion.setText("1.0");
        }
    }

    private void saveAccountChanges() {
        String uid = FirebaseRefs.getAuth().getUid();
        if (uid == null) return;

        String name = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_update_failed, ""), Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseRefs.usersCollection().document(uid)
                .update("name", name, "phone", phone)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, R.string.account_updated_message, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_update_failed, e.getMessage()), Toast.LENGTH_LONG).show());
    }

    private void launchDeleteAccountEmailApp() {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + practiceContactEmail));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.delete_account_email_subject));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.contact_no_email_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void logout() {
        FirebaseRefs.getAuth().signOut();
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}