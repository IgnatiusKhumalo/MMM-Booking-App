package com.mmm.bookingapp.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.mmm.bookingapp.R;
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.ValidationUtils;

/**
 * Screen 13 - Change Password Screen.
 * Requires re-authentication with the current password before allowing
 * the change, per Firebase's security requirement for sensitive account
 * actions - prevents someone with brief access to an unlocked, logged-in
 * phone from changing the password and locking out the real owner.
 */
public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etCurrentPassword, etNewPassword, etConfirmNewPassword;
    private Button btnSubmit;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        btnSubmit = findViewById(R.id.btnSubmitChangePassword);
        progressBar = findViewById(R.id.progressBar);

        btnSubmit.setOnClickListener(v -> attemptChangePassword());
    }

    private void attemptChangePassword() {
        String currentPassword = etCurrentPassword.getText().toString();
        String newPassword = etNewPassword.getText().toString();
        String confirmNewPassword = etConfirmNewPassword.getText().toString();

        if (!ValidationUtils.isValidPassword(newPassword)) {
            Toast.makeText(this, R.string.error_password, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ValidationUtils.doPasswordsMatch(newPassword, confirmNewPassword)) {
            Toast.makeText(this, R.string.error_confirm_password, Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseRefs.getAuth().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        setLoading(true);
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> user.updatePassword(newPassword)
                        .addOnSuccessListener(unused2 -> {
                            setLoading(false);
                            Toast.makeText(this, R.string.password_changed_message, Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            setLoading(false);
                            Toast.makeText(this, getString(R.string.error_change_password_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                        }))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, R.string.error_current_password_wrong, Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
    }
}