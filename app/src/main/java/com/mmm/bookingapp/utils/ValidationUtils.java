package com.mmm.bookingapp.utils;

import java.util.regex.Pattern;

/**
 * Centralises all input validation rules used across Register, Login,
 * Change Password, and Book Appointment, per the Project Structure
 * document. Kept deliberately simple and readable.
 */
public class ValidationUtils {

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
        if (email == null) return false;
        return email.contains("@");
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
     * Compares two email addresses for equality, case-insensitively and
     * with surrounding whitespace trimmed - email addresses are treated
     * as case-insensitive in virtually every real-world provider, so a
     * confirm-email field should not reject "Name@Example.com" vs
     * "name@example.com" as a mismatch.
     */
    public static boolean doEmailsMatch(String email1, String email2) {
        if (email1 == null || email2 == null) return false;
        return email1.trim().equalsIgnoreCase(email2.trim());
    }
}