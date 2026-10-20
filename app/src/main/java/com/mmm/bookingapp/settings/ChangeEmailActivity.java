package com.mmm.bookingapp.settings;

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
 * Change Email Screen (Settings).
 *
 * Lets a logged-in client move their account to a different email address
 * - for example if the registered one is lost or may be compromised. This
 * only works while the client is still signed in and still knows their
 * password. If a client loses BOTH their password and access to their
 * email, no self-service recovery exists here or in any email/password
 * system; the fallback is to contact the practice so an admin can assist
 * through the Firebase Console.
 *
 * It follows ChangePasswordActivity's pattern: the current password is
 * confirmed first (re-authentication), then Firebase's
 * verifyBeforeUpdateEmail() is called. That does NOT change the login
 * email straight away - it sends a confirmation link to the NEW address,
 * and the email only changes once that link is clicked. This stops
 * someone taking over an account by entering an address that is not
 * really theirs.
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