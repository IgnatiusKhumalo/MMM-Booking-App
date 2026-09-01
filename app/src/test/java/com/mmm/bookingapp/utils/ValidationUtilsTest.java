package com.mmm.bookingapp.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Real unit tests for ValidationUtils, replacing the untouched Android
 * Studio default test file. 27 test cases covering every validation
 * rule and its edge cases.
 */
public class ValidationUtilsTest {

    // ---------- isValidFullName ----------
    @Test public void fullName_validTwoWordName_returnsTrue() {
        assertTrue(ValidationUtils.isValidFullName("Thandi Mokoena"));
    }
    @Test public void fullName_validThreeWordName_returnsTrue() {
        assertTrue(ValidationUtils.isValidFullName("Thandi Nolwazi Mokoena"));
    }
    @Test public void fullName_singleWord_returnsFalse() {
        assertFalse(ValidationUtils.isValidFullName("Thandi"));
    }
    @Test public void fullName_null_returnsFalse() {
        assertFalse(ValidationUtils.isValidFullName(null));
    }
    @Test public void fullName_empty_returnsFalse() {
        assertFalse(ValidationUtils.isValidFullName(""));
    }
    @Test public void fullName_singleLetterSecondWord_returnsFalse() {
        assertFalse(ValidationUtils.isValidFullName("Thandi M"));
    }
    @Test public void fullName_extraWhitespaceBetweenWords_returnsTrue() {
        assertTrue(ValidationUtils.isValidFullName("Thandi   Mokoena"));
    }
    @Test public void fullName_leadingTrailingWhitespace_returnsTrue() {
        assertTrue(ValidationUtils.isValidFullName("  Thandi Mokoena  "));
    }
    @Test public void fullName_onlyWhitespace_returnsFalse() {
        assertFalse(ValidationUtils.isValidFullName("   "));
    }

    // ---------- isValidEmail ----------
    @Test public void email_containsAtSymbol_returnsTrue() {
        assertTrue(ValidationUtils.isValidEmail("thandi@example.com"));
    }
    @Test public void email_noAtSymbol_returnsFalse() {
        assertFalse(ValidationUtils.isValidEmail("thandi.example.com"));
    }
    @Test public void email_null_returnsFalse() {
        assertFalse(ValidationUtils.isValidEmail(null));
    }
    @Test public void email_empty_returnsFalse() {
        assertFalse(ValidationUtils.isValidEmail(""));
    }
    @Test public void email_onlyAtSymbol_returnsTrue() {
        // Documents the actual (intentionally simple) rule: contains "@" only.
        assertTrue(ValidationUtils.isValidEmail("@"));
    }

    // ---------- isValidPhoneRemainder ----------
    @Test public void phone_exactlyNineDigits_returnsTrue() {
        assertTrue(ValidationUtils.isValidPhoneRemainder("721234567"));
    }
    @Test public void phone_eightDigits_returnsFalse() {
        assertFalse(ValidationUtils.isValidPhoneRemainder("72123456"));
    }
    @Test public void phone_tenDigits_returnsFalse() {
        assertFalse(ValidationUtils.isValidPhoneRemainder("7212345678"));
    }
    @Test public void phone_containsLetters_returnsFalse() {
        assertFalse(ValidationUtils.isValidPhoneRemainder("72123abc7"));
    }
    @Test public void phone_null_returnsFalse() {
        assertFalse(ValidationUtils.isValidPhoneRemainder(null));
    }
    @Test public void phone_empty_returnsFalse() {
        assertFalse(ValidationUtils.isValidPhoneRemainder(""));
    }

    // ---------- isValidPassword ----------
    @Test public void password_meetsAllRules_returnsTrue() {
        assertTrue(ValidationUtils.isValidPassword("Passw0rd!"));
    }
    @Test public void password_tooShort_returnsFalse() {
        assertFalse(ValidationUtils.isValidPassword("Pw0!"));
    }
    @Test public void password_noUppercase_returnsFalse() {
        assertFalse(ValidationUtils.isValidPassword("passw0rd!"));
    }
    @Test public void password_noLowercase_returnsFalse() {
        assertFalse(ValidationUtils.isValidPassword("PASSW0RD!"));
    }
    @Test public void password_noNumber_returnsFalse() {
        assertFalse(ValidationUtils.isValidPassword("Password!"));
    }
    @Test public void password_noSpecialCharacter_returnsFalse() {
        assertFalse(ValidationUtils.isValidPassword("Passw0rd"));
    }
    @Test public void password_null_returnsFalse() {
        assertFalse(ValidationUtils.isValidPassword(null));
    }

    // ---------- doPasswordsMatch ----------
    @Test public void passwordsMatch_identicalStrings_returnsTrue() {
        assertTrue(ValidationUtils.doPasswordsMatch("Passw0rd!", "Passw0rd!"));
    }
    @Test public void passwordsMatch_differentStrings_returnsFalse() {
        assertFalse(ValidationUtils.doPasswordsMatch("Passw0rd!", "Passw0rd@"));
    }
    @Test public void passwordsMatch_firstNull_returnsFalse() {
        assertFalse(ValidationUtils.doPasswordsMatch(null, "Passw0rd!"));
    }
}