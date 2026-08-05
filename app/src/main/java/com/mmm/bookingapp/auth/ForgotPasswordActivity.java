package com.mmm.bookingapp.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mmm.bookingapp.R;
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.ValidationUtils;

/**
 * Screen 5 - Forgot Password Screen.
 * Sends a Firebase-managed password reset email. The confirmation message
 * shown is deliberately identical whether or not the email is actually
 * registered, per standard security practice - this screen must not reveal
 * which email addresses exist in the system.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private TextView tvConfirmation;
    private Button btnSendReset;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etEmail);
        tvConfirmation = findViewById(R.id.tvConfirmation);
        btnSendReset = findViewById(R.id.btnSendReset);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnSendReset.setOnClickListener(v -> attemptSendReset());
    }

    private void attemptSendReset() {
        String email = etEmail.getText().toString().trim();

        if (!ValidationUtils.isValidEmail(email)) {
            Toast.makeText(this, R.string.error_email, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        FirebaseRefs.getAuth().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    // Shown regardless of task.isSuccessful() for the account-enumeration
                    // reason documented in the class comment above.
                    setLoading(false);
                    tvConfirmation.setVisibility(View.VISIBLE);
                    btnSendReset.setEnabled(false);
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSendReset.setEnabled(!loading);
    }
}