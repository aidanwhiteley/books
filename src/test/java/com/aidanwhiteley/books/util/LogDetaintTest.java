package com.aidanwhiteley.books.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogDetaintTest {

    @Test
    void shouldRemoveTaintedChars() {
        final String input ="abc\ndef\rghi\tjkl";
        final String expectedOutput = "abc_def_ghi_jkl";

        assertEquals(expectedOutput, LogDetaint.logMessageDetaint(input));
    }
}
