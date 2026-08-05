package com.mmm.bookingapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mmm.bookingapp.R;
import com.mmm.bookingapp.models.User;
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.ValidationUtils;

/**
 * Screen 3 - Register Screen.
 * Full implementation per the Blueprint: validates all fields using the
 * shared ValidationUtils rules, creates the Firebase Auth account, writes
 * the matching Firestore "users" document with role hardcoded to "client"
 * (never user-selectable - the Register screen has no role field at all,
 * matching the documented security requirement), and triggers Firebase's
 * built-in email verification.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPhone, etPassword, etConfirmPassword;
    private TextView tvFullNameError, tvEmailError, tvPhoneError, tvConfirmPasswordError;
    private TextView tvPasswordRequirements;
    private Button btnSubmit;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        tvFullNameError = findViewById(R.id.tvFullNameError);
        tvEmailError = findViewById(R.id.tvEmailError);
        tvPhoneError = findViewById(R.id.tvPhoneError);
        tvConfirmPasswordError = findViewById(R.id.tvConfirmPasswordError);
        tvPasswordRequirements = findViewById(R.id.tvPasswordRequirements);

        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Real-time password feedback, per the Walkthrough's documented behaviour
        // ("the app tells her in real time as she types whether she's met each rule").
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                boolean valid = ValidationUtils.isValidPassword(s.toString());
                tvPasswordRequirements.setTextColor(
                        valid ? getColor(R.color.color_status_confirmed) : getColor(R.color.color_text_primary)
                );
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSubmit.setOnClickListener(v -> attemptRegistration());
    }

    private void attemptRegistration() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phoneRemainder = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        boolean isValid = true;

        if (!ValidationUtils.isValidFullName(fullName)) {
            tvFullNameError.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            tvFullNameError.setVisibility(View.GONE);
        }

        if (!ValidationUtils.isValidEmail(email)) {
            tvEmailError.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            tvEmailError.setVisibility(View.GONE);
        }

        if (!ValidationUtils.isValidPhoneRemainder(phoneRemainder)) {
            tvPhoneError.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            tvPhoneError.setVisibility(View.GONE);
        }

        if (!ValidationUtils.isValidPassword(password)) {
            Toast.makeText(this, R.string.error_password, Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (!ValidationUtils.doPasswordsMatch(password, confirmPassword)) {
            tvConfirmPasswordError.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            tvConfirmPasswordError.setVisibility(View.GONE);
        }

        if (!isValid) return;

        String fullPhone = "+27" + phoneRemainder;
        setLoading(true);
        createFirebaseAccount(fullName, email, fullPhone, password);
    }

    private void createFirebaseAccount(String fullName, String email, String fullPhone, String password) {
        FirebaseAuth auth = FirebaseRefs.getAuth();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        setLoading(false);
                        Toast.makeText(this, R.string.error_registration_failed, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = firebaseUser.getUid();
                    User newUser = new User(uid, fullName, email, fullPhone);
                    // role, emailVerified, notificationsEnabled all default correctly
                    // inside the User constructor - see models/User.java

                    FirebaseRefs.usersCollection().document(uid).set(newUser)
                            .addOnSuccessListener(unused -> sendVerificationEmail(firebaseUser))
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this,
                                        getString(R.string.error_registration_failed, e.getMessage()),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            getString(R.string.error_registration_failed, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void sendVerificationEmail(FirebaseUser firebaseUser) {
        firebaseUser.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    showSuccessDialog();
                });
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.registration_success_title)
                .setMessage(R.string.registration_success_message)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                    // Sign out immediately - account exists but is unverified;
                    // routing to Login matches the Walkthrough exactly
                    // ("she checks her email... returns to app and logs in").
                    FirebaseRefs.getAuth().signOut();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
    }
}