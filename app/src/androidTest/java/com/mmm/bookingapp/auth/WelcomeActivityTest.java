package com.mmm.bookingapp.auth;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mmm.bookingapp.R;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Real Espresso UI test on the actual Welcome Screen (Screen 2),
 * replacing the untouched Android Studio default instrumented test.
 * Confirms the app's true entry point renders correctly: both the
 * Register and Log In buttons are present, visible, and correctly
 * labelled - the two only ways into the app, per the Blueprint.
 */
@RunWith(AndroidJUnit4.class)
public class WelcomeActivityTest {

    @Test
    public void welcomeScreen_displaysRegisterAndLoginButtons() {
        try (ActivityScenario<WelcomeActivity> scenario = ActivityScenario.launch(WelcomeActivity.class)) {
            onView(withId(R.id.btnRegister))
                    .check(matches(isDisplayed()))
                    .check(matches(withText(R.string.btn_register)));

            onView(withId(R.id.btnLogin))
                    .check(matches(isDisplayed()))
                    .check(matches(withText(R.string.btn_login)));
        }
    }
}