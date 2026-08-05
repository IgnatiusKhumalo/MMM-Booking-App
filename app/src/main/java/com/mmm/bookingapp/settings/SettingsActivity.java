package com.mmm.bookingapp.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.content.pm.PackageInfo;
import android.widget.Button;
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
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.ThemeManager;

/**
 * Screen 12 - Settings Screen (client version).
 * The admin-only "Practice Information" section is added on top of this
 * same screen in a later batch, once AdminDashboardActivity and
 * PracticeInfoActivity are built - per the Blueprint, both roles share
 * this Settings screen, with the extra section shown only when role == admin.
 *
 * Logout is now available in three places: a top-of-screen red button
 * (visible immediately, no scrolling required), plus the original
 * Security and Support section buttons further down.
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
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) ->
                ThemeManager.setDarkModeEnabled(this, isChecked)
        );

        findViewById(R.id.btnSaveChanges).setOnClickListener(v -> saveAccountChanges());
        findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> openDeleteAccountEmail());
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

        Button btnLogoutTop = findViewById(R.id.btnLogoutTop);
        Button btnLogoutSecurity = findViewById(R.id.btnLogoutSecurity);
        Button btnLogoutSupport = findViewById(R.id.btnLogoutSupport);
        btnLogoutTop.setOnClickListener(v -> logout());
        btnLogoutSecurity.setOnClickListener(v -> logout());
        btnLogoutSupport.setOnClickListener(v -> logout());
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
                });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            FirebaseRefs.usersCollection().document(uid)
                    .update("notificationsEnabled", isChecked);
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
        // On failure, the hardcoded fallback above is used - defensive,
        // per the documented safe-fallback pattern.
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
     * Delete My Account - deliberately a static contact instruction, not a
     * database-backed request queue, per the agreed simplification: opens
     * the device's email app pre-addressed to the practice. Actual deletion
     * is completed manually by the developer via the Firebase Console,
     * as documented.
     */
    private void openDeleteAccountEmail() {
        Toast.makeText(this, R.string.delete_account_instructions, Toast.LENGTH_LONG).show();

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