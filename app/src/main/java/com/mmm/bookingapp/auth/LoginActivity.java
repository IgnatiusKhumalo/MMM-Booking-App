package com.mmm.bookingapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mmm.bookingapp.R;
import com.mmm.bookingapp.admin.AdminDashboardActivity;
import com.mmm.bookingapp.client.HomeActivity;
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.NetworkStatusHelper;

/**
 * Screen 4 - Login Screen (shared, role-aware).
 * One single login flow for both Client and Admin, per the Blueprint -
 * there is no separate admin login screen or admin registration path
 * anywhere in this app. After successful Firebase authentication and an
 * email-verification check, this reads the user's "role" field from
 * Firestore and routes to HomeActivity (client) or AdminDashboardActivity
 * (admin) accordingly.
 *
 * Fresh sign-in ALWAYS requires connectivity - there is no way to verify
 * a password offline, in any app. Connectivity is checked before
 * attempting sign-in, so the user gets a clear explanation instead of
 * Firebase's raw network error text. Note this only applies to a fresh
 * login attempt; a user who is already signed in from a previous session
 * is routed straight past this screen by MainActivity's cached-session
 * check, without needing connectivity.
 *
 * Includes a show/hide password toggle (eye icon) so the user can
 * visually confirm what they've typed before submitting.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLoginSubmit, btnResendVerification;
    private TextView tvForgotPassword;
    private View layoutUnverified;
    private ProgressBar progressBar;
    private ImageButton btnTogglePassword;
    private boolean passwordVisible = false;

    // Held between a failed (unverified) login attempt and a resend tap,
    // so we don't need the user to re-enter their password just to resend.
    private FirebaseUser pendingUnverifiedUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLoginSubmit = findViewById(R.id.btnLoginSubmit);
        btnResendVerification = findViewById(R.id.btnResendVerification);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        layoutUnverified = findViewById(R.id.layoutUnverified);
        progressBar = findViewById(R.id.progressBar);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class))
        );

        btnLoginSubmit.setOnClickListener(v -> attemptLogin());
        btnResendVerification.setOnClickListener(v -> resendVerificationEmail());
    }

    /**
     * Toggles etPassword between hidden (dots) and visible (plain text),
     * swapping the icon to match, and keeps the cursor at the end of the
     * text rather than jumping to the start.
     */
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_eye);
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private void attemptLogin() {
        // Checked first, before any field validation, so offline users get
        // this specific explanation rather than filling in the form only
        // to see a confusing network error afterwards.
        if (!NetworkStatusHelper.isOnline(this)) {
            Toast.makeText(this, R.string.error_offline_login, Toast.LENGTH_LONG).show();
            return;
        }

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.error_login_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        layoutUnverified.setVisibility(View.GONE);
        setLoading(true);

        FirebaseRefs.getAuth().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        setLoading(false);
                        return;
                    }

                    // Force-refresh verification status from Firebase's servers,
                    // rather than trusting a possibly-stale local cache.
                    firebaseUser.reload().addOnCompleteListener(reloadTask -> {
                        if (!firebaseUser.isEmailVerified()) {
                            setLoading(false);
                            pendingUnverifiedUser = firebaseUser;
                            layoutUnverified.setVisibility(View.VISIBLE);
                            FirebaseRefs.getAuth().signOut();
                            return;
                        }
                        routeByRole(firebaseUser.getUid());
                    });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    if (!NetworkStatusHelper.isOnline(this)) {
                        // Connectivity dropped between the initial check and
                        // now - same honest message rather than raw error text.
                        Toast.makeText(this, R.string.error_offline_login, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                                getString(R.string.error_login_failed, e.getMessage()),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void routeByRole(String uid) {
        FirebaseRefs.usersCollection().document(uid).get()
                .addOnSuccessListener(document -> {
                    setLoading(false);
                    if (!document.exists()) {
                        Toast.makeText(this, R.string.error_user_record_missing, Toast.LENGTH_LONG).show();
                        return;
                    }

                    String role = document.getString("role");
                    Intent intent;
                    if ("admin".equals(role)) {
                        intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, HomeActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, R.string.error_user_record_missing, Toast.LENGTH_LONG).show();
                });
    }

    private void resendVerificationEmail() {
        if (pendingUnverifiedUser == null) return;
        pendingUnverifiedUser.sendEmailVerification()
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, R.string.verification_email_resent, Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                getString(R.string.error_login_failed, e.getMessage()),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLoginSubmit.setEnabled(!loading);
    }
}