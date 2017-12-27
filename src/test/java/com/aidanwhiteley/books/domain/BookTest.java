package com.aidanwhiteley.books.domain;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertTrue;

/**
 * Some basic tests really only checking how Lombok is working (and in the IntelliJ IDE...)
 */
public class BookTest {

    public static final String TEST_TITLE = "Test Title";
    public static final String AIDAN = "Aidan";
    public static final String WHODUNNIT = "Whodunnit";
    public static final String LE_CARRE = "Le Carre";
    public static final String SOMEONE_DUNNIT = "Someone dunnit";
    public static final String WHO_DID_IT = "Who did it?";
    public static final Book.Rating GREAT = Book.Rating.GREAT;
    public static final LocalDate NOW = LocalDate.now();

    @Test
    public void testCreateBook() {
        Book testBook = new Book();
        testBook.setTitle(TEST_TITLE);
        assertTrue(testBook.toString().contains(TEST_TITLE));
    }

    @Test
    public void testBuildBook() {
        Book testBook = Book.builder().author(AIDAN).genre(WHODUNNIT).lastRead(NOW).rating(GREAT)
                .similarTo(LE_CARRE).summary(SOMEONE_DUNNIT).title(WHO_DID_IT).build();

        assertTrue(testBook.toString().contains(AIDAN));
        assertTrue(testBook.getGenre() == WHODUNNIT);

        Book testBookWithoutAllFields = Book.builder().author(AIDAN).genre(WHODUNNIT).lastRead(NOW).rating(GREAT).build();
        assertTrue(testBookWithoutAllFields.getGenre() == WHODUNNIT);
    }
}