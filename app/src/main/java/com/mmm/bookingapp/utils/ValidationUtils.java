package com.mmm.bookingapp.utils;

import java.util.regex.Pattern;

/**
 * Central validation rules for the app, matching the exact requirements
 * locked in the Blueprint document (Section 3, Screen 3 - Register Screen).
 * Every screen that validates user input must call these methods rather
 * than writing its own rules, so validation stays consistent app-wide.
 */
public class ValidationUtils {

    // At least 2 words, each word at least 2 characters (rejects single names, initials)
    public static boolean isValidFullName(String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        String[] words = trimmed.split("\\s+");
        if (words.length < 2) return false;
        for (String word : words) {
            if (word.length() < 2) return false;
        }
        return true;
    }

    // Must contain "@" - matches the Blueprint's stated rule exactly (kept simple,
    // not over-engineered with a full RFC email regex, per the documented requirement).
    public static boolean isValidEmail(String email) {
        return email != null && email.contains("@");
    }

    // Phone is entered without the +27 prefix (prefix is fixed/non-editable in the UI),
    // so this only validates the remaining digits: must be numeric, reasonable length.
    public static boolean isValidPhoneRemainder(String phoneAfterPrefix) {
        if (phoneAfterPrefix == null) return false;
        return phoneAfterPrefix.matches("\\d{9}");
    }

    // 8+ characters, 1 uppercase, 1 lowercase, 1 number, 1 special character
    // - matches the Blueprint's exact password rule set.
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%&!]).{8,}$"
    );

    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    public static boolean doPasswordsMatch(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }
}