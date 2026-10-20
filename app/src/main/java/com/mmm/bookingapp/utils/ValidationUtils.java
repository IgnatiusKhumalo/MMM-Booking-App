package com.mmm.bookingapp.utils;

import java.util.regex.Pattern;

/**
 * Centralises the input validation rules used by Register, Login,
 * Change Password, Change Email and Book Appointment, per the Project
 * Structure document. Plain Java with no Android dependencies, so every
 * rule is covered by fast unit tests (ValidationUtilsTest).
 */
public class ValidationUtils {

    /**
     * An email must have a local part, one "@", and a domain with at least
     * one dot (for example name@example.com). Firebase Authentication also
     * validates the address when the account is created.
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    public static boolean isValidFullName(String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return false;
        String[] parts = trimmed.split("\\s+");
        if (parts.length < 2) return false;
        for (String part : parts) {
            if (part.length() < 2) return false;
        }
        return true;
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhoneRemainder(String phone) {
        if (phone == null) return false;
        return Pattern.matches("\\d{9}", phone);
    }

    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        if (password.length() < 8) return false;
        boolean hasUpper = Pattern.compile("[A-Z]").matcher(password).find();
        boolean hasLower = Pattern.compile("[a-z]").matcher(password).find();
        boolean hasDigit = Pattern.compile("\\d").matcher(password).find();
        boolean hasSpecial = Pattern.compile("[^A-Za-z0-9]").matcher(password).find();
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    public static boolean doPasswordsMatch(String password, String confirmPassword) {
        if (password == null || confirmPassword == null) return false;
        return password.equals(confirmPassword);
    }

    /**
     * Compares two email addresses for equality, ignoring case and
     * surrounding whitespace - email addresses are treated as
     * case-insensitive by virtually every real provider, so a confirm-email
     * field should not call "Name@Example.com" and "name@example.com" a
     * mismatch.
     */
    public static boolean doEmailsMatch(String email1, String email2) {
        if (email1 == null || email2 == null) return false;
        return email1.trim().equalsIgnoreCase(email2.trim());
    }
}