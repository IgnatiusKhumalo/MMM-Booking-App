package com.mmm.bookingapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.mmm.bookingapp.R;

/**
 * Screen 2 - Welcome Screen.
 * The app's single entry point (Blueprint, Section 3). No back arrow -
 * this is a root screen; the system back button/gesture exits the app
 * from here, per the documented navigation rule.
 */
public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnLogin = findViewById(R.id.btnLogin);

        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class))
        );

        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class))
        );
    }
}