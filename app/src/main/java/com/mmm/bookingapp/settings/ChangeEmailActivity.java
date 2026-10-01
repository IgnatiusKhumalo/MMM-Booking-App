package com.mmm.bookingapp.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.mmm.bookingapp.R;
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.ValidationUtils;

/**
 * Change Email Screen (Settings). Added specifically so a client who
 * suspects their registered email may be lost or compromised can secure
 * their account immediately, WHILE they are still logged in and still
 * know their password - the one window where self-service recovery is
 * actually possible. If a client loses both their password AND access
 * to their email at the same time, no self-service option exists here
 * or anywhere else (this is universal to all email/password login
 * systems) - the documented fallback is contacting the practice
 * directly so an admin can assist via the Firebase Console.
 *
 * Mirrors ChangePasswordActivity's exact re-authentication pattern:
 * the current password must be confirmed first (proves it's really the
 * account owner), before Firebase's verifyBeforeUpdateEmail() is called.
 * That method does NOT change the account's login email immediately -
 * it sends a confirmation link to the NEW address first, and the email
 * only actually changes once that link is clicked. This prevents someone
 * from silently taking over an account by entering a new email that
 * isn't really theirs.
 */
public class ChangeEmailActivity extends AppCompatActivity {

    private EditText etCurrentPasswordForEmail, etNewEmail, etConfirmNewEmail;
    private Button btnSubmitChangeEmail;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_email);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        etCurrentPasswordForEmail = findViewById(R.id.etCurrentPasswordForEmail);
        etNewEmail = findViewById(R.id.etNewEmail);
        etConfirmNewEmail = findViewById(R.id.etConfirmNewEmail);
        btnSubmitChangeEmail = findViewById(R.id.btnSubmitChangeEmail);
        progressBar = findViewById(R.id.progressBar);

        btnSubmitChangeEmail.setOnClickListener(v -> attemptChangeEmail());
    }

    private void attemptChangeEmail() {
        String currentPassword = etCurrentPasswordForEmail.getText().toString();
        String newEmail = etNewEmail.getText().toString().trim();
        String confirmNewEmail = etConfirmNewEmail.getText().toString().trim();

        if (currentPassword.isEmpty()) {
            Toast.makeText(this, R.string.error_current_password_wrong, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ValidationUtils.isValidEmail(newEmail)) {
            Toast.makeText(this, R.string.error_email, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ValidationUtils.doEmailsMatch(newEmail, confirmNewEmail)) {
            Toast.makeText(this, R.string.error_confirm_email, Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseRefs.getAuth().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) return;

        setLoading(true);

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(unused -> currentUser.verifyBeforeUpdateEmail(newEmail)
                        .addOnSuccessListener(unused2 -> {
                            setLoading(false);
                            showSuccessDialog();
                        })
                        .addOnFailureListener(e -> {
                            setLoading(false);
                            Toast.makeText(this,
                                    getString(R.string.error_change_email_failed, e.getMessage()),
                                    Toast.LENGTH_LONG).show();
                        }))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, R.string.error_current_password_wrong, Toast.LENGTH_LONG).show();
                });
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.change_email_success_title)
                .setMessage(R.string.change_email_success_message)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> finish())
                .show();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmitChangeEmail.setEnabled(!loading);
    }
}