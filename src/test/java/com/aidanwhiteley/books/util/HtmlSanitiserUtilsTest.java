package com.aidanwhiteley.books.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HtmlSanitiserUtilsTest {

    private final String TEST_STRING_1 =
            """
                <p>Here is some text in a para</p>
                <p><b>Bold text</b></p>
                <p>Text with <em>em</em> and <i>i</i></p>
                <p>Text with a <br>
                  line break</p>
                """;

    private final String TEST_STRING_2 =
            """
                <p>Here is some text in a para</p>
                <p><b>Bold text</b></p>
                <p>Text with <em>em</em> and <i>i</i></p>
                <script>alert(1)</script>
                <p>Text with a <br>
                <a onmouseover="alert(1)">Click me!</a>
                <p>&#60&#115&#99&#114&#105&#112&#116&#62&#97&#108&#101&#114&#116&#40&#49&#41&#60&#47&#115&#99&#114&#105&#112&#116&#62</p>
                  line break</p>
                """;

    private final String TEST_STRING_3 =
            """
            <p>No tags allowed</p>
            """;


    @Test
    void testTagsExpectedInGoogleBookDescriptionsAllowed() {
        String sanitised = HtmlSanitiserUtils.allowBasicTextFormattingOnly(TEST_STRING_1);
        assertEquals(TEST_STRING_1.trim(), sanitised.trim());
    }

    @Test
    void testTagsNotExpectedInGoogleBookDescriptionsAllowed() {
        String sanitised = HtmlSanitiserUtils.allowBasicTextFormattingOnly(TEST_STRING_2);
        assertFalse(sanitised.contains("<a"));
        assertFalse(sanitised.contains("onmouseover"));
        assertFalse(sanitised.contains("<script>"));
        assertFalse(sanitised.contains("&#60&#115"));
    }

    @Test
    void testTNoTagssAllowed() {
        String sanitised = HtmlSanitiserUtils.allowNoHtmlTags(TEST_STRING_3);
        assertEquals("No tags allowed", sanitised.trim());
    }


}
