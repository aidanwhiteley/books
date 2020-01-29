package com.aidanwhiteley.books.util;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LogDetaintTest {

    @Test
    public void shouldRemoveTaintedChars() {
        final String input ="abc\ndef\rghi\tjkl";
        final String expectedOutput = "abc_def_ghi_jkl";

        assertEquals(expectedOutput, LogDetaint.logMessageDetaint(input));
    }
}
