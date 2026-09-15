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

import com.mmm.bookingapp.R;
import com.mmm.bookingapp.auth.WelcomeActivity;
import com.mmm.bookingapp.client.ContactActivity;
import com.mmm.bookingapp.models.PracticeInfo;
import com.mmm.bookingapp.utils.DialogUtils;
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.ThemeManager;

/**
 * Screen 12 - Settings Screen (client version).
 *
 * Now shows a toast confirming Dark Mode and Booking Reminder toggle
 * changes; Logout goes through a Cancel/Log Out confirmation dialog;
 * Delete My Account now shows a clear explainer dialog (what to expect,
 * that the practice will confirm by email) before launching the email
 * app, replacing the previous brief Toast-only instruction.
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

                    Boolean notificationsEnabled = document.getBoolean("notificationsEnabled");
                    switchNotifications.setChecked(notificationsEnabled == null || notificationsEnabled);

                    String role = document.getString("role");
                    if ("admin".equals(role)) {
                        findViewById(R.id.layoutAdminSection).setVisibility(android.view.View.VISIBLE);
                        findViewById(R.id.btnPracticeInfo).setOnClickListener(v ->
                                startActivity(new Intent(this, com.mmm.bookingapp.admin.PracticeInfoActivity.class)));
                    }
                });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            FirebaseRefs.usersCollection().document(uid)
                    .update("notificationsEnabled", isChecked);
            Toast.makeText(this,
                    isChecked ? R.string.notifications_enabled_message : R.string.notifications_disabled_message,
                    Toast.LENGTH_SHORT).show();
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

    /**
     * The actual email-launch step, now separated from the explainer
     * dialog above it (DialogUtils.showDeleteAccountExplainer calls this
     * only after the client taps "Continue" and understands the process).
     */
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