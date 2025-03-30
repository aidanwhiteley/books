package com.aidanwhiteley.books.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HtmlSanitiserUtilsTest {


    @Test
    void testTagsExpectedInGoogleBookDescriptionsAllowed() {
        final String testString1 = """
                <p>Here is some text in a para</p>
                <p><b>Bold text</b></p>
                <p>Text with <em>em</em> and <i>i</i></p>
                <p>Text with a <br>
                  line break</p>
                """;
        String sanitised = HtmlSanitiserUtils.allowBasicTextFormattingOnly(testString1);
        assertEquals(testString1.trim(), sanitised.trim());
    }

    @Test
    void testTagsNotExpectedInGoogleBookDescriptionsAllowed() {
        final String testString2 = """
                <p>Here is some text in a para</p>
                <p><b>Bold text</b></p>
                <p>Text with <em>em</em> and <i>i</i></p>
                <script>alert(1)</script>
                <p>Text with a <br>
                <a onmouseover="alert(1)">Click me!</a>
                <p>&#60&#115&#99&#114&#105&#112&#116&#62&#97&#108&#101&#114&#116&#40&#49&#41&#60&#47&#115&#99&#114&#105&#112&#116&#62</p>
                  line break</p>
                """;
        String sanitised = HtmlSanitiserUtils.allowBasicTextFormattingOnly(testString2);
        assertFalse(sanitised.contains("<a"));
        assertFalse(sanitised.contains("onmouseover"));
        assertFalse(sanitised.contains("<script>"));
        assertFalse(sanitised.contains("&#60&#115"));
    }

    @Test
    void testTNoTagsAllowed() {
        final String testString3 = """
                <p>No tags allowed</p>
                """;
        String sanitised = HtmlSanitiserUtils.allowNoHtmlTags(testString3);
        assertEquals("No tags allowed", sanitised.trim());
    }


}
