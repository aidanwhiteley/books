package com.aidanwhiteley.books.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientInputSanitiserUtilsTest {

    @Test
    void testAllAlphaNumericCharacters() {
        String input = "1a2B3c4D5e6FyZ";
        String result = ClientInputSanitiserUtils.sanitiseGoogleBookId(input);
        assertEquals("1a2B3c4D5e6FyZ", result);
    }

    @Test
    void testSWithNonHexCharacters() {
        String input = "12gH!@#34zaZ";
        String result = ClientInputSanitiserUtils.sanitiseGoogleBookId(input);
        assertEquals("12gH34zaZ", result);
    }

    @Test
    void testNullInput() {
        String result = ClientInputSanitiserUtils.sanitiseGoogleBookId(null);
        assertEquals("", result);
    }
}