package com.mmm.bookingapp.auth;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mmm.bookingapp.R;
import com.mmm.bookingapp.models.User;
import com.mmm.bookingapp.settings.AboutActivity;
import com.mmm.bookingapp.settings.PrivacyPolicyActivity;
import com.mmm.bookingapp.settings.TermsActivity;
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.ValidationUtils;

/**
 * Screen 3 - Register Screen.
 *
 * Validates every field (name, email plus a matching confirm-email field,
 * phone, password plus confirm-password, and the compulsory consent
 * checkbox) and shows each problem next to its field. The password rules
 * line turns green as the password becomes valid, and red and bold if the
 * form is submitted while it is not. The confirm-email field catches a
 * typo made differently in each box; it cannot catch the same wrong
 * address typed into both, which no confirm field can.
 *
 * Registration is two steps: create the Firebase Authentication account,
 * then write the matching Firestore profile document. If the second step
 * fails, the Authentication account is deleted again (rolled back), so a
 * failed registration never leaves a login that has no profile and could
 * never get into the app.
 *
 * Log messages deliberately contain no email addresses.
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText etFullName, etEmail, etConfirmEmail, etPhone, etPassword, etConfirmPassword;
    private TextView tvFullNameError, tvEmailError, tvConfirmEmailError, tvPhoneError, tvConfirmPasswordError;
    private TextView tvPasswordRequirements, tvAgreeTerms, tvAgreeTermsError;
    private CheckBox cbAgreeTerms, cbSubscribe;
    private Button btnSubmit;
    private ProgressBar progressBar;

    private ImageButton btnTogglePassword, btnToggleConfirmPassword;
    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etConfirmEmail = findViewById(R.id.etConfirmEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        tvFullNameError = findViewById(R.id.tvFullNameError);
        tvEmailError = findViewById(R.id.tvEmailError);
        tvConfirmEmailError = findViewById(R.id.tvConfirmEmailError);
        tvPhoneError = findViewById(R.id.tvPhoneError);
        tvConfirmPasswordError = findViewById(R.id.tvConfirmPasswordError);
        tvPasswordRequirements = findViewById(R.id.tvPasswordRequirements);
        tvAgreeTerms = findViewById(R.id.tvAgreeTerms);
        tvAgreeTermsError = findViewById(R.id.tvAgreeTermsError);

        cbAgreeTerms = findViewById(R.id.cbAgreeTerms);
        cbSubscribe = findViewById(R.id.cbSubscribe);

        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        btnToggleConfirmPassword.setOnClickListener(v -> toggleConfirmPasswordVisibility());

        setupAgreeTermsLinks();

        // Live feedback: the rules line is green once the password is valid.
        // Typing also clears the red/bold error styling set by a failed submit.
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                boolean valid = ValidationUtils.isValidPassword(s.toString());
                tvPasswordRequirements.setTypeface(null, Typeface.NORMAL);
                tvPasswordRequirements.setTextColor(
                        valid ? getColor(R.color.color_status_confirmed) : getColor(R.color.color_text_primary)
                );
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSubmit.setOnClickListener(v -> attemptRegistration());
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

    private void toggleConfirmPasswordVisibility() {
        confirmPasswordVisible = !confirmPasswordVisible;
        if (confirmPasswordVisible) {
            etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnToggleConfirmPassword.setImageResource(R.drawable.ic_eye_off);
        } else {
            etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnToggleConfirmPassword.setImageResource(R.drawable.ic_eye);
        }
        etConfirmPassword.setSelection(etConfirmPassword.getText().length());
    }

    private void setupAgreeTermsLinks() {
        String about = getString(R.string.register_agree_about);
        String privacy = getString(R.string.register_agree_privacy);
        String terms = getString(R.string.register_agree_terms);

        String fullText = getString(R.string.register_agree_prefix) + " " + about + ", " + privacy + " "
                + getString(R.string.register_agree_and) + " " + terms;

        SpannableString spannable = new SpannableString(fullText);
        int linkColor = getColor(R.color.color_primary);

        addClickableSpan(spannable, fullText, about, linkColor, () ->
                startActivity(new Intent(this, AboutActivity.class)));
        addClickableSpan(spannable, fullText, privacy, linkColor, () ->
                startActivity(new Intent(this, PrivacyPolicyActivity.class)));
        addClickableSpan(spannable, fullText, terms, linkColor, () ->
                startActivity(new Intent(this, TermsActivity.class)));

        tvAgreeTerms.setText(spannable);
        tvAgreeTerms.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void addClickableSpan(SpannableString spannable, String fullText, String target,
                                  int color, Runnable onClick) {
        int start = fullText.indexOf(target);
        if (start == -1) return;
        int end = start + target.length();

        ClickableSpan span = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                onClick.run();
            }
            @Override
            public void updateDrawState(android.text.TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(color);
                ds.setUnderlineText(true);
            }
        };
        spannable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void attemptRegistration() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String confirmEmail = etConfirmEmail.getText().toString().trim();
        String phoneRemainder = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        Log.d(TAG, "Registration attempt");

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

        if (!ValidationUtils.doEmailsMatch(email, confirmEmail)) {
            tvConfirmEmailError.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            tvConfirmEmailError.setVisibility(View.GONE);
        }

        if (!ValidationUtils.isValidPhoneRemainder(phoneRemainder)) {
            tvPhoneError.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            tvPhoneError.setVisibility(View.GONE);
        }

        if (!ValidationUtils.isValidPassword(password)) {
            // Inline feedback: highlight the password rules line instead of showing a toast.
            tvPasswordRequirements.setTextColor(getColor(R.color.color_error));
            tvPasswordRequirements.setTypeface(null, Typeface.BOLD);
            isValid = false;
        }

        if (!ValidationUtils.doPasswordsMatch(password, confirmPassword)) {
            tvConfirmPasswordError.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            tvConfirmPasswordError.setVisibility(View.GONE);
        }

        if (!cbAgreeTerms.isChecked()) {
            tvAgreeTermsError.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            tvAgreeTermsError.setVisibility(View.GONE);
        }

        if (!isValid) {
            Log.w(TAG, "Registration blocked - validation failed");
            return;
        }

        String fullPhone = "+27" + phoneRemainder;
        boolean marketingOptIn = cbSubscribe.isChecked();
        setLoading(true);
        createFirebaseAccount(fullName, email, fullPhone, password, marketingOptIn);
    }

    private void createFirebaseAccount(String fullName, String email, String fullPhone,
                                       String password, boolean marketingOptIn) {
        FirebaseAuth auth = FirebaseRefs.getAuth();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        setLoading(false);
                        Log.e(TAG, "Firebase user was null after account creation");
                        Toast.makeText(this, R.string.error_registration_failed, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = firebaseUser.getUid();
                    User newUser = new User(uid, fullName, email, fullPhone, marketingOptIn);

                    FirebaseRefs.usersCollection().document(uid).set(newUser)
                            .addOnSuccessListener(unused -> {
                                Log.i(TAG, "Registration successful for uid: " + uid);
                                sendVerificationEmail(firebaseUser);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to write user document for uid: " + uid, e);
                                rollbackAuthAccount(firebaseUser, e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "Firebase account creation failed", e);
                    Toast.makeText(this,
                            getString(R.string.error_registration_failed, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * The Authentication account exists but its Firestore profile could
     * not be written. Delete the Authentication account so the user is not
     * left with a login that has no profile (which could never get into
     * the app), then tell them registration failed so they can try again.
     */
    private void rollbackAuthAccount(FirebaseUser firebaseUser, String failureMessage) {
        firebaseUser.delete().addOnCompleteListener(deleteTask -> {
            if (deleteTask.isSuccessful()) {
                Log.i(TAG, "Rolled back incomplete registration");
            } else {
                Log.e(TAG, "Could not roll back incomplete registration", deleteTask.getException());
            }
            FirebaseRefs.getAuth().signOut();
            setLoading(false);
            Toast.makeText(this,
                    getString(R.string.error_registration_failed, failureMessage),
                    Toast.LENGTH_LONG).show();
        });
    }

    private void sendVerificationEmail(FirebaseUser firebaseUser) {
        firebaseUser.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    Log.d(TAG, "Verification email sent: " + task.isSuccessful());
                    showSuccessDialog();
                });
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.registration_success_title)
                .setMessage(R.string.registration_success_message)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
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