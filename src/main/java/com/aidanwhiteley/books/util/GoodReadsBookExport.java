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

    // Multiline string not used because of additional whitespace it can introduce
    private static final String HEADER_ROW_PART1 = "Book Id,Title,Author,Author l-f,Additional Authors,ISBN,ISBN13,My Rating,";
    private static final String HEADER_ROW_PART2 = "Average Rating,Publisher,Binding,Number of Pages,Year Published,";
    private static final String HEADER_ROW_PART3 = "Original Publication Year,Date Read,Date Added,Bookshelves,";
    private static final String HEADER_ROW_PART4 = "Bookshelves with positions,Exclusive Shelf,My Review,Spoiler,Private Notes,";
    private static final String HEADER_ROW_PART5 = "Read Count,Owned Copies";
    private static final String HEADER_ROW = HEADER_ROW_PART1 + HEADER_ROW_PART2 + HEADER_ROW_PART3 + HEADER_ROW_PART4 + HEADER_ROW_PART5;

    private GoodReadsBookExport() {
        // Private - only static utility methods supported
    }

    public static String goodReadsExportHeaderRow() {
        return removeNewLines(HEADER_ROW).trim();
    }

    public static String goodReadsExportAsCsv(Book book) {

        var bookExport = new StringBuilder();
        bookExport.append(book.getId()).append(DELIMTER);
        bookExport.append(encloseInDoubleQuotes(escapeDoubleQuote(book.getTitle()))).append(DELIMTER);
        bookExport.append(encloseInDoubleQuotes(escapeDoubleQuote(book.getAuthor()))).append(DELIMTER);
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
        // Exclusive shelf is hard coded to read
        bookExport.append("read").append(DELIMTER);
        // Sigh - Good Reads dont appear to output the "My Review" field!
        bookExport.append(EMPTY_FIELD).append(DELIMTER);
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

            // Good Reads follows US date pattern
            return book.getCreatedDateTime().getYear() + "/" +
                    month + "/" +
                    day;
        } else {
            // A very basic fallback implementation
            return LocalDateTime.now().getYear() + "/" + LocalDateTime.now().getMonthValue() +
                    "/" + LocalDateTime.now().getDayOfMonth();
        }
    }

    private static String getRating(Book book) {
        var rating = book.getRating();
        // Cloudy use a range of 0 to 4 where as uses 1 to 5;
        return (rating.getRatingLevel() + 1) + "";
    }

    // So here's an example of what these two wierd fields look like in an actual export
    // from Good Reads
    // ,"=""0752889516""","=""9780752889511""",
    private static StringBuilder getIsbnNumbers(Book book, StringBuilder bookExport) {
        if (book.getGoogleBookDetails() != null && book.getGoogleBookDetails().getVolumeInfo() != null
            && book.getGoogleBookDetails().getVolumeInfo().getIndustryIdentifiers() != null) {
            var isbn10 = book.getGoogleBookDetails().getVolumeInfo().getIndustryIdentifiers().
                    stream().filter(s -> s.getType().equals(IndustryIdentifiers.TYPE_ISBN_10)).
                    map(s -> s.getIdentifier()).
                    findFirst().orElse(DELIMTER);
            bookExport.append("\"=\"\"").append(isbn10).append("\"\"\"").append(DELIMTER);

            var isbn13 = book.getGoogleBookDetails().getVolumeInfo().getIndustryIdentifiers().
                    stream().filter(s -> s.getType().equals(IndustryIdentifiers.TYPE_ISBN_13)).
                    map(s -> s.getIdentifier()).
                    findFirst().orElse(DELIMTER);
            bookExport.append("\"=\"\"").append(isbn13).append("\"\"\"").append(DELIMTER);
        }
        return bookExport;
    }

    private static String encloseInDoubleQuotes(String field) {
        if (field == null) {
            return SPACE;
        }

        if (field.indexOf(DELIMTER) > -1) {
            return DOUBLE_QUOTE + field.replace(DOUBLE_QUOTE, "") + DOUBLE_QUOTE;
        } else {
            return field;
        }
    }

    private static String escapeDoubleQuote(String field) {
        if (field == null) {
            return SPACE;
        }

        return field.replace("\"", "\"\"");
    }

    private static String removeNewLines(String field) {
        var output = field.replace("\r\n", SPACE);
        return output.replace("\n", SPACE);
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
