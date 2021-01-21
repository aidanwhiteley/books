package com.aidanwhiteley.books.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Some basic tests really only checking how Lombok is working (and in the IntelliJ IDE...)
 */
class BookTest {

    private static final String TEST_TITLE = "Test Title";
    private static final String AIDAN = "Aidan";
    private static final String WHODUNNIT = "Whodunnit";
    private static final String SOMEONE_DUNNIT = "Someone dunnit";
    private static final String WHO_DID_IT = "Who did it?";
    private static final Book.Rating GREAT = Book.Rating.GREAT;
    private static final LocalDateTime NOW = LocalDateTime.now();

    @Test
    void testCreateBook() {
        Book testBook = new Book();
        testBook.setTitle(TEST_TITLE);
        assertTrue(testBook.toString().contains(TEST_TITLE));
    }

    @Test
    void testBuildBook() {
        Book testBook = Book.builder().author(AIDAN).genre(WHODUNNIT).createdDateTime(NOW).rating(GREAT).
                summary(SOMEONE_DUNNIT).title(WHO_DID_IT).build();

        assertTrue(testBook.toString().contains(AIDAN));
        assertEquals(WHODUNNIT, testBook.getGenre());

        Book testBookWithoutAllFields = Book.builder().author(AIDAN).genre(WHODUNNIT).createdDateTime(NOW).rating(GREAT).build();
        assertEquals(WHODUNNIT, testBookWithoutAllFields.getGenre());
    }

    @Test
    void testGetRatingByString() {
        assertEquals(Book.Rating.POOR, Book.Rating.getRatingByString("poor"));
        assertNotEquals(Book.Rating.POOR, Book.Rating.getRatingByString("DoesntExist"));
        assertNull(Book.Rating.getRatingByString("DoesntExist"));
        assertEquals(Book.Rating.GREAT, Book.Rating.getRatingByString("gReAt"));
    }
    
    @Test
    void testBoilerPlateMethods() {
    	Book book1 = Book.builder().author(AIDAN).genre(WHODUNNIT).createdDateTime(NOW).rating(GREAT).
                summary(SOMEONE_DUNNIT).title(WHO_DID_IT).build();
    	Book book2 = Book.builder().author(AIDAN).genre(WHODUNNIT).createdDateTime(NOW).rating(GREAT).
                summary(SOMEONE_DUNNIT).title(WHO_DID_IT).build();
    	
    	assertEquals(book1.hashCode(), book2.hashCode());
        assertEquals(book1, book2);
    	assertEquals(book1.toString(), book2.toString());
    }
    
}