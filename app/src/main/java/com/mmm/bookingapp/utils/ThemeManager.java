package com.mmm.bookingapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Centralises Light/Dark theme logic, per the Project Structure document.
 * The preference is stored locally on-device only (SharedPreferences) and
 * is never synced to Firestore - it is a personal display setting, not
 * practice data, per the Blueprint's Section 1.
 */
public class ThemeManager {

    private static final String PREFS_NAME = "mmm_theme_prefs";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    public static boolean isDarkModeEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public static void setDarkModeEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
        AppCompatDelegate.setDefaultNightMode(
                enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    /** Called once at app startup (see MMMApplication) to restore the saved preference. */
    public static void applySavedTheme(Context context) {
        boolean darkMode = isDarkModeEnabled(context);
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}