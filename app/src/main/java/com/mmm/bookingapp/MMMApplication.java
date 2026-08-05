package com.mmm.bookingapp;

import android.app.Application;
import com.mmm.bookingapp.utils.ThemeManager;

/**
 * Custom Application class - applies the user's saved theme preference at
 * app startup, before any Activity is created, so Dark Mode takes effect
 * immediately rather than requiring a manual app restart.
 */
public class MMMApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.applySavedTheme(this);
    }
}