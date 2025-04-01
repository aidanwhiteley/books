package com.aidanwhiteley.books.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class HtmlSanitiserUtils {

    private HtmlSanitiserUtils() {
        throw new UnsupportedOperationException("Should only be called through static methods!");
    }

    public static String allowNoHtmlTags(String text) {
        Safelist safelist = Safelist.none();
        return Jsoup.clean(text, safelist);
    }

    public static String allowBasicTextFormattingOnly(String text) {
        Safelist safelist = Safelist.simpleText();
        safelist.addTags("p", "br", "em", "i");
        return Jsoup.clean(text, safelist);
    }
}
