package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.googlebooks.IndustryIdentifiers;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import com.aidanwhiteley.books.domain.googlebooks.VolumeInfo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GoodReadsBookExportTest {

    public static String MULTI_LINE_SUMMARY = """
            Here is a multiline string
            with data on more than one line.
            """;

    public static String SUMMARY_WITH_DOUBLE_QOUTE = "Here is \"some\" text";

    @Test
    void testTitleWithQuotes() {
        var export = getTestBookAsGoodReadsExport();
        assertTrue(export.contains("George R.R. Martin"));
    }

    @Test
    void testAuthorLastFirst() {
        var export = getTestBookAsGoodReadsExport();
        assertTrue(export.contains("\"Martin, George R.R.\""));
    }

    @Test
    void testGoodReadsRating() {
        var export = getTestBookAsGoodReadsExport();
        assertTrue(export.contains(",5,"));
    }

    @Test
    void testReadDate() {
        var export = getTestBookAsGoodReadsExport();
        assertTrue(export.contains(LocalDateTime.now().getYear()+ "/" ));
    }

    @Test
    void testNewLinesRemoved() {
        var header = GoodReadsBookExport.goodReadsExportHeaderRow();
        assertTrue(header.indexOf("\r\n") == -1);
        assertTrue(header.indexOf("\n") == -1);

        var export = getTestBookAsGoodReadsExport();
        assertTrue(export.indexOf("\r\n") == -1);
        assertTrue(export.indexOf("\n") == -1);
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
