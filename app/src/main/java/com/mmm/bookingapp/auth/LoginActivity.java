package com.mmm.bookingapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
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
 * Unchanged functionally from the previous revision; Log calls added at
 * key points (attempt, success, failure, unverified block) for the
 * Logging and Monitoring requirement.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword;
    private Button btnLoginSubmit, btnResendVerification;
    private TextView tvForgotPassword;
    private View layoutUnverified;
    private ProgressBar progressBar;
    private ImageButton btnTogglePassword;
    private boolean passwordVisible = false;

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
        if (!NetworkStatusHelper.isOnline(this)) {
            Log.w(TAG, "Login blocked - device offline");
            Toast.makeText(this, R.string.error_offline_login, Toast.LENGTH_LONG).show();
            return;
        }

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.error_login_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.i(TAG, "Login attempt for: " + email);
        layoutUnverified.setVisibility(View.GONE);
        setLoading(true);

        FirebaseRefs.getAuth().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        setLoading(false);
                        return;
                    }

                    firebaseUser.reload().addOnCompleteListener(reloadTask -> {
                        if (!firebaseUser.isEmailVerified()) {
                            Log.w(TAG, "Login blocked - email not verified: " + email);
                            setLoading(false);
                            pendingUnverifiedUser = firebaseUser;
                            layoutUnverified.setVisibility(View.VISIBLE);
                            FirebaseRefs.getAuth().signOut();
                            return;
                        }
                        Log.i(TAG, "Login successful: " + email);
                        routeByRole(firebaseUser.getUid());
                    });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    if (!NetworkStatusHelper.isOnline(this)) {
                        Log.w(TAG, "Login failed - connectivity lost mid-attempt");
                        Toast.makeText(this, R.string.error_offline_login, Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "Login failed for " + email, e);
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
                        Log.e(TAG, "User record missing for uid: " + uid);
                        Toast.makeText(this, R.string.error_user_record_missing, Toast.LENGTH_LONG).show();
                        return;
                    }

                    String role = document.getString("role");
                    Log.d(TAG, "Routing by role: " + role);
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
                    Log.e(TAG, "Failed to read user record for role routing", e);
                    Toast.makeText(this, R.string.error_user_record_missing, Toast.LENGTH_LONG).show();
                });
    }

    private void resendVerificationEmail() {
        if (pendingUnverifiedUser == null) return;
        pendingUnverifiedUser.sendEmailVerification()
                .addOnSuccessListener(unused -> {
                    Log.i(TAG, "Verification email resent");
                    Toast.makeText(this, R.string.verification_email_resent, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to resend verification email", e);
                    Toast.makeText(this,
                            getString(R.string.error_login_failed, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLoginSubmit.setEnabled(!loading);
    }
}