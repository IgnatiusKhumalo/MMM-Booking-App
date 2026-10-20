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

import com.google.firebase.auth.FirebaseUser;
import com.mmm.bookingapp.R;
import com.mmm.bookingapp.admin.AdminDashboardActivity;
import com.mmm.bookingapp.client.HomeActivity;
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.NetworkStatusHelper;
import com.mmm.bookingapp.utils.SessionCache;

/**
 * Screen 4 - Login Screen (shared by clients and admins, routed by role).
 *
 * An empty email or password is flagged on the field itself. A fresh
 * login is blocked with a clear message when the device is offline, and
 * blocked until the account's email is verified. After a successful login
 * the account's role is cached on the device (SessionCache) and the user
 * is sent to the matching home screen with a "showLoginSuccess" flag, so
 * that screen shows a brief "Logged in successfully" toast.
 * MainActivity's returning-session path does not set the flag, so
 * reopening the app does not repeat the message.
 *
 * Log messages deliberately contain no email addresses.
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
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Flag every empty field on the field itself, before any network check.
        boolean missingField = false;
        if (email.isEmpty()) {
            etEmail.setError(getString(R.string.error_field_required));
            missingField = true;
        }
        if (password.isEmpty()) {
            etPassword.setError(getString(R.string.error_field_required));
            missingField = true;
        }
        if (missingField) return;

        if (!NetworkStatusHelper.isOnline(this)) {
            Log.w(TAG, "Login blocked - device offline");
            Toast.makeText(this, R.string.error_offline_login, Toast.LENGTH_LONG).show();
            return;
        }

        Log.i(TAG, "Login attempt");
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
                            Log.w(TAG, "Login blocked - email not verified for uid: " + firebaseUser.getUid());
                            setLoading(false);
                            pendingUnverifiedUser = firebaseUser;
                            layoutUnverified.setVisibility(View.VISIBLE);
                            FirebaseRefs.getAuth().signOut();
                            return;
                        }
                        Log.i(TAG, "Login successful for uid: " + firebaseUser.getUid());
                        routeByRole(firebaseUser.getUid());
                    });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    if (!NetworkStatusHelper.isOnline(this)) {
                        Log.w(TAG, "Login failed - connectivity lost mid-attempt");
                        Toast.makeText(this, R.string.error_offline_login, Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "Login failed", e);
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
                    SessionCache.saveRole(this, uid, role);

                    Intent intent;
                    if ("admin".equals(role)) {
                        intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, HomeActivity.class);
                    }
                    intent.putExtra("showLoginSuccess", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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