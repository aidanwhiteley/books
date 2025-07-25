package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.googlebooks.IndustryIdentifiers;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import com.aidanwhiteley.books.domain.googlebooks.VolumeInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GoodReadsBookExportTest {

    public static final String MULTI_LINE_SUMMARY = """
            Here is a multiline string
            with data on more than one line.
            """;

    public static final String SUMMARY_WITH_DOUBLE_QOUTE = "Here is \"some\" text";

    @ParameterizedTest
    @ValueSource(strings = {"George R.R. Martin", "\"Martin, George R.R.\"", "\",5,"})
    void testExpectedContents(String arg) {
        var export = getTestBookAsGoodReadsExport();
        assertTrue(export.contains(arg));
    }

    @Test
    void testReadDate() {
        var export = getTestBookAsGoodReadsExport();
        assertTrue(export.contains(LocalDateTime.now().getYear()+ "/" ));
    }

    @Test
    void testNewLinesRemoved() {
        var header = GoodReadsBookExport.goodReadsExportHeaderRow();
        assertEquals(-1, header.indexOf("\r\n"));
        assertEquals(-1, header.indexOf("\n"));

        var export = getTestBookAsGoodReadsExport();
        assertEquals(-1, export.indexOf("\r\n"));
        assertEquals(-1, export.indexOf("\n"));
    }

    private String getTestBookAsGoodReadsExport() {
        var book = createTestBook();
        return GoodReadsBookExport.goodReadsExportAsCsv(book);
    }

    private static Book createTestBook() {
        var book = new Book();
        book.setTitle("First \"second\" third");
        book.setAuthor("George R.R. Martin");
        book.setSummary(MULTI_LINE_SUMMARY);

        var item = new Item();
        var volumeInfo = new VolumeInfo();
        var industryIdentifiersList = new ArrayList<IndustryIdentifiers>();
        var isbn10 = new IndustryIdentifiers(IndustryIdentifiers.TYPE_ISBN_10, "anISBN10");
        var isbn13 = new IndustryIdentifiers(IndustryIdentifiers.TYPE_ISBN_13, "anISBN13");
        industryIdentifiersList.add(isbn10);
        industryIdentifiersList.add(isbn13);
        volumeInfo.setIndustryIdentifiers(industryIdentifiersList);
        item.setVolumeInfo(volumeInfo);
        book.setGoogleBookDetails(item);

        book.setRating(Book.Rating.GREAT);
        book.setCreatedDateTime(LocalDateTime.now());
        return book;
    }
}
