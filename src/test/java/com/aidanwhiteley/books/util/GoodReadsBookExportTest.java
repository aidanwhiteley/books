package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import com.aidanwhiteley.books.domain.googlebooks.VolumeInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * I couldn't find any documentation of the GoodReads export format so this code mimics a couple of
 * Gists found online and also the export / import functionality on StoryGraph.
 */
public class GoodReadsBookExportTest {

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

    private String getTestBookAsGoodReadsExport() {
        var book = new Book();
        book.setTitle("First \"second\" third");
        book.setAuthor("George R.R. Martin");

        var item = new Item();
        var volumeInfo = new VolumeInfo();

        var outputAsCsv = GoodReadsBookExport.goodReadsExportAsCsv(book);
        System.out.println(outputAsCsv);
        return outputAsCsv;
    }
}
