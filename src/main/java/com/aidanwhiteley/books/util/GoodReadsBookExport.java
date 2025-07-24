package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.googlebooks.IndustryIdentifiers;

import java.time.LocalDateTime;

/**
 * I couldn't find any documentation of the GoodReads export format so this code mimics a couple of
 * Gists found online and also the export / import functionality on StoryGraph.
 */
public class GoodReadsBookExport {

    private static final String DELIMTER = ",";
    private static final String COMMA = ",";
    private static final String DOUBLE_QUOTE = "\"";
    private static final String SPACE = " ";
    private static final String EMPTY_FIELD = "";

    private static final String HEADER_ROW = """
        Book Id,Title,Author,Author l-f,Additional Authors,ISBN,ISBN13,My Rating,Average Rating,Publisher,
        Binding,Number of Pages,Year Published,Original Publication Year,Date Read,Date Added,
        Bookshelves,Bookshelves with positions,Exclusive Shelf,My Review,Spoiler,Private Notes,
        Read Count,Owned Copies
    """;

    private GoodReadsBookExport() {
        // Private - only static utility methods supported
    }

    public static String goodReadsExportHeaderRow() {
        return removeNewLines(HEADER_ROW).trim();
    }

    public static String goodReadsExportAsCsv(Book book) {

        var bookExport = new StringBuilder();
        bookExport.append(book.getId()).append(DELIMTER);
        bookExport.append(encloseInDoubleQuotes(book.getTitle())).append(DELIMTER);
        bookExport.append(encloseInDoubleQuotes(book.getAuthor())).append(DELIMTER);
        bookExport.append(lastFirst(book.getAuthor())).append(DELIMTER);
        // Additional Authors not supported
        bookExport.append(EMPTY_FIELD).append(DELIMTER);
        bookExport = getIsbnNumbers(book, bookExport);
        bookExport.append(getRating(book)).append(DELIMTER);
        // Average rating not supported
        bookExport.append(EMPTY_FIELD).append(DELIMTER);
        // Publisher not supported
        bookExport.append(EMPTY_FIELD).append(DELIMTER);
        // Binding not supported
        bookExport.append(EMPTY_FIELD).append(DELIMTER);
        // Number of pages not supported
        bookExport.append(EMPTY_FIELD).append(DELIMTER);
        // Year published not supported
        bookExport.append(EMPTY_FIELD).append(DELIMTER);
        // Original publication year not supported
        bookExport.append(EMPTY_FIELD).append(DELIMTER);
        // We set both Date Read and Date Added to when the review was created on Cloudy
        bookExport.append(getReviewCreatedData(book)).append(DELIMTER);
        bookExport.append(getReviewCreatedData(book)).append(DELIMTER);
        // Bookshelves not supported
        bookExport.append(EMPTY_FIELD).append(DELIMTER);
        // Bookshelves with positions not supported
        bookExport.append(EMPTY_FIELD).append(DELIMTER);
        // Exclusive shelf not supported
        bookExport.append(EMPTY_FIELD).append(DELIMTER);
        bookExport.append(removeNewLines(encloseInDoubleQuotes(book.getSummary()))).append(DELIMTER);
        // Spoiler shelf not supported
        bookExport.append(EMPTY_FIELD).append(DELIMTER);
        // Private notes not supported
        bookExport.append(EMPTY_FIELD).append(DELIMTER);
        // Read count and Owned copies always both defaulted to 1
        bookExport.append("1").append(DELIMTER);
        bookExport.append("1");

        return bookExport.toString();
    }

    private static String getReviewCreatedData(Book book) {
        if (book.getCreatedDateTime() != null) {
            var day = book.getCreatedDateTime().getDayOfMonth() < 10 ?
                    "0" + book.getCreatedDateTime().getDayOfMonth() : book.getCreatedDateTime().getDayOfMonth();
            var month = book.getCreatedDateTime().getMonthValue() < 10 ?
                    "0" + book.getCreatedDateTime().getMonthValue() : book.getCreatedDateTime().getMonthValue();
            return day + "/" +
                    month + "/" +
                    book.getCreatedDateTime().getYear();
        } else {
            // A very basic fallback implementation
            return LocalDateTime.now().getDayOfMonth() + "/" + LocalDateTime.now().getMonthValue() +
                    "/" + LocalDateTime.now().getYear();
        }
    }

    private static String getRating(Book book) {
        var rating = book.getRating();
        // Cloudy 0 to 4. GoodReads 1 to 5;
        return (rating.getRatingLevel() + 1) + "";
    }

    private static StringBuilder getIsbnNumbers(Book book, StringBuilder bookExport) {
        if (book.getGoogleBookDetails() != null && book.getGoogleBookDetails().getVolumeInfo() != null
            && book.getGoogleBookDetails().getVolumeInfo().getIndustryIdentifiers() != null) {
            var isbn10 = book.getGoogleBookDetails().getVolumeInfo().getIndustryIdentifiers().
                    stream().filter(s -> s.getType().equals(IndustryIdentifiers.TYPE_ISBN_10)).
                    map(s -> s.getIdentifier()).
                    findFirst().orElse("");
            bookExport.append(isbn10).append(DELIMTER);

            var isbn13 = book.getGoogleBookDetails().getVolumeInfo().getIndustryIdentifiers().
                    stream().filter(s -> s.getType().equals(IndustryIdentifiers.TYPE_ISBN_13)).
                    map(s -> s.getIdentifier()).
                    findFirst().orElse("");
            bookExport.append(isbn13).append(DELIMTER);
        }
        return bookExport;
    }

    private static String encloseInDoubleQuotes(String field) {
        if (field == null) {
            return SPACE;
        }

        if (field.indexOf(DELIMTER) > -1) {
            return DOUBLE_QUOTE + field.replaceAll(DOUBLE_QUOTE, "") + DOUBLE_QUOTE;
        } else {
            return field;
        }
    }

    private static String removeNewLines(String field) {
        var output = field.replaceAll("\r\n", SPACE);
        return output.replaceAll("\n", SPACE);
    }

    private static String lastFirst(String field) {
        if (field == null || field.indexOf(SPACE) == -1) {
            return SPACE;
        }
        int lastSpace = field.lastIndexOf(SPACE);
        return encloseInDoubleQuotes(field.substring(lastSpace).trim() + COMMA +
                SPACE + field.substring(0, lastSpace));
    }

}
