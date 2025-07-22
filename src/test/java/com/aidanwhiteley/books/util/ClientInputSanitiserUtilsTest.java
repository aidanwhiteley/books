package com.aidanwhiteley.books.util;

import org.junit.jupiter.api.Test;

import static com.aidanwhiteley.books.util.ClientInputSanitiserUtils.sanitiseGoogleBookId;
import static com.aidanwhiteley.books.util.ClientInputSanitiserUtils.isValidTitleOrAuthor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientInputSanitiserUtilsTest {

    @Test
    void testGoogleBookIdAllAlphaNumericCharacters() {
        String input = "1a2B3c4D5e6FyZ";
        String result = sanitiseGoogleBookId(input);
        assertEquals("1a2B3c4D5e6FyZ", result);
    }

    @Test
    void testGoogleBookIdWithNonAlphaNumericCharacters() {
        String input = "12gH!@#34zaZ";
        String result = sanitiseGoogleBookId(input);
        assertEquals("12gH34zaZ", result);
    }

    @Test
    void testGoogleBookIdNullInput() {
        String result = sanitiseGoogleBookId(null);
        assertEquals("", result);
    }

    @Test
    void testIsValidTitleOrAuthorValidInput() {
        assertTrue(isValidTitleOrAuthor("A Good Book Title"));
    }

    @Test
    void testIsValidTitleOrAuthorNullInput() {
        assertFalse(isValidTitleOrAuthor(null));
    }

    @Test
    void testIsValidTitleOrAuthorTooShort() {
        assertFalse(isValidTitleOrAuthor(""));
    }

    @Test
    void testIsValidTitleOrAuthorTooLong() {
        String longInput = "a".repeat(151);
        assertFalse(isValidTitleOrAuthor(longInput));
    }

    @Test
    void testIsValidTitleOrAuthorContainsNewLine() {
        assertFalse(isValidTitleOrAuthor("Title with\nnewline"));
    }

    @Test
    void testIsValidTitleOrAuthorContainsCarriageReturn() {
        assertFalse(isValidTitleOrAuthor("Title with\rcarriage return"));
    }

    @Test
    void testIsValidTitleOrAuthorContainsHttp() {
        assertFalse(isValidTitleOrAuthor("http://malicious.link"));
    }

    @Test
    void testIsValidTitleOrAuthorContainsHttps() {
        assertFalse(isValidTitleOrAuthor("https://malicious.link"));
    }
}