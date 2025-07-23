package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.domain.Book;

public class GoodReadsBookExport {

    private static final String DELIMTER = ",";

    private static final String HEADER_ROW = """
        Book Id,Title,Author,Author l-f,Additional Authors,ISBN,ISBN13,My Rating,Average Rating,Publisher,
        Binding,Number of Pages,Year Published,Original Publication Year,Date Read,Date Added,
        Bookshelves,Bookshelves with positions,Exclusive Shelf,My Review,Spoiler,Private Notes,
        Read Count,Owned Copies
    """;

    private GoodReadsBookExport() {
        // Private - only static utitlity methods supported
    }

    public static String goodReadsExportAsCsv(Book book) {

        var bookExport = new StringBuilder();


        return null;
    }
}
