package com.aidanwhiteley.books.repository.dtos;

import com.aidanwhiteley.books.domain.Book;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BooksByRatingTest {

    @Test
    void testEqualsHashcode() {
        BooksByRating by = new BooksByRating(Book.Rating.POOR, 1);
        assertNotEquals(new BooksByRating(Book.Rating.POOR, 2), by);
        assertEquals(new BooksByRating(Book.Rating.POOR, 1), by);

        assertNotEquals(by.hashCode(), new BooksByRating(Book.Rating.POOR, 2).hashCode());
        assertEquals(by.hashCode(), new BooksByRating(Book.Rating.POOR, 1).hashCode());
    }
}
