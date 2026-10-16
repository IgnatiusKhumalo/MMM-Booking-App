package com.mmm.bookingapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Remembers each account's role ("client" or "admin") on this device so
 * the splash screen can still open the right home screen when the role
 * cannot be fetched (for example offline, with no cached Firestore copy).
 *
 * The cached role is used ONLY to choose which screen opens. It grants no
 * access: what a screen may read or write is decided by the Firestore
 * Security Rules on the server, so a tampered cache could at most open an
 * empty screen.
 */
public class SessionCache {

    private static final String PREFS_NAME = "mmm_session_cache";
    private static final String KEY_ROLE_PREFIX = "role_";

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void saveRole(Context context, String uid, String role) {
        if (uid == null || role == null) return;
        prefs(context).edit().putString(KEY_ROLE_PREFIX + uid, role).apply();
    }

    /** The cached role for this account, or null if none has been saved yet. */
    public static String getRole(Context context, String uid) {
        if (uid == null) return null;
        return prefs(context).getString(KEY_ROLE_PREFIX + uid, null);
    }
}