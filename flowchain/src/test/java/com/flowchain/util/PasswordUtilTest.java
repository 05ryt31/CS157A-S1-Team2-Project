package com.flowchain.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void hash_producesBcryptString() {
        String hash = PasswordUtil.hash("correct horse battery staple");

        assertNotNull(hash);
        assertEquals(60, hash.length(), "bcrypt strings are 60 chars");
        assertTrue(hash.startsWith("$2"), "bcrypt prefix expected, got: " + hash);
    }

    @Test
    void hash_producesDifferentOutputForSamePassword() {
        // Each call must generate a fresh salt — otherwise rainbow tables work.
        String a = PasswordUtil.hash("hunter2");
        String b = PasswordUtil.hash("hunter2");

        assertNotEquals(a, b);
    }

    @Test
    void hash_rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(null));
    }

    @Test
    void hash_rejectsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(""));
    }

    @Test
    void verify_returnsTrueForMatchingPassword() {
        String hash = PasswordUtil.hash("s3cret!");

        assertTrue(PasswordUtil.verify("s3cret!", hash));
    }

    @Test
    void verify_returnsFalseForWrongPassword() {
        String hash = PasswordUtil.hash("s3cret!");

        assertFalse(PasswordUtil.verify("S3cret!", hash), "case-sensitive check");
        assertFalse(PasswordUtil.verify("s3cret", hash));
        assertFalse(PasswordUtil.verify("totally different", hash));
    }

    @Test
    void verify_returnsFalseForNullInputs() {
        String hash = PasswordUtil.hash("anything");

        assertFalse(PasswordUtil.verify(null, hash));
        assertFalse(PasswordUtil.verify("anything", null));
        assertFalse(PasswordUtil.verify(null, null));
    }

    @Test
    void verify_returnsFalseForMalformedHash() {
        // Mock-data rows store plaintext; verify must return false, not throw.
        assertFalse(PasswordUtil.verify("password123", "password123"));
        assertFalse(PasswordUtil.verify("x", ""));
        assertFalse(PasswordUtil.verify("x", "not-a-bcrypt-string"));
    }

    @Test
    void verify_handlesAsciiSpecialChars() {
        String pw = "P@ssw0rd! with spaces & symbols #$%";
        String hash = PasswordUtil.hash(pw);

        assertTrue(PasswordUtil.verify(pw, hash));
        assertFalse(PasswordUtil.verify(pw + " ", hash), "trailing space must not match");
    }
}
