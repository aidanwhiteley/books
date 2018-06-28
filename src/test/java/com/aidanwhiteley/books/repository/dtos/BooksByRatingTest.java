package com.aidanwhiteley.books.repository.dtos;

import com.aidanwhiteley.books.domain.Book;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class BooksByRatingTest {

    @Test
    public void testEqualsHashcode() {
        BooksByRating by = new BooksByRating(Book.Rating.POOR, 1);
        assertNotEquals(by, new BooksByRating(Book.Rating.POOR, 2));
        assertEquals(by, new BooksByRating(Book.Rating.POOR, 1));

        assertNotEquals(by.hashCode(), new BooksByRating(Book.Rating.POOR, 2).hashCode());
        assertEquals(by.hashCode(), new BooksByRating(Book.Rating.POOR, 1).hashCode());
    }
}
