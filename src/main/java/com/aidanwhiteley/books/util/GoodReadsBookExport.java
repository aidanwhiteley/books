package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.domain.Book;

public class GoodReadsBookExport {

    private static final String DELIMTER = ",";
    private static final String DOUBLE_QUOTE = "\"";
    private static final String SPACE = " ";

    private static final String HEADER_ROW = """
        Book Id,Title,Author,Author l-f,Additional Authors,ISBN,ISBN13,My Rating,Average Rating,Publisher,
        Binding,Number of Pages,Year Published,Original Publication Year,Date Read,Date Added,
        Bookshelves,Bookshelves with positions,Exclusive Shelf,My Review,Spoiler,Private Notes,
        Read Count,Owned Copies
    """;

    private GoodReadsBookExport() {
        // Private - only static utility methods supported
    }

    public static String goodReadsExportAsCsv(Book book) {

        var bookExport = new StringBuilder();
        bookExport.append(book.getId()).append(DELIMTER);
        bookExport.append(encloseInDoubleQuotes(book.getTitle())).append(DELIMTER);
        bookExport.append(book.getAuthor()).append(DELIMTER);
        bookExport.append(lastFirst(book.getAuthor())).append(DELIMTER);

        return bookExport.toString();
    }

    private static String encloseInDoubleQuotes(String field) {
        if (field == null) {
            return SPACE;
        }
        return DOUBLE_QUOTE + field.replaceAll(DOUBLE_QUOTE, "") + DOUBLE_QUOTE;
    }

    private static String lastFirst(String field) {
        if (field == null || field.indexOf(SPACE) == -1) {
            return SPACE;
        }
        int lastSpace = field.lastIndexOf(SPACE);
        return DOUBLE_QUOTE + field.substring(lastSpace).trim() + DELIMTER + SPACE +
                field.substring(0, lastSpace) + DOUBLE_QUOTE;
    }
}
