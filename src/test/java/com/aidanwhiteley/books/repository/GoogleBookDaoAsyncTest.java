package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.Assert.assertNull;

public class GoogleBookDaoAsyncTest extends IntegrationTest {

    @Autowired
    BookRepository bookRepository;

    @Test
    public void testBookUpdatedWithGoogleBookDetails() {
        Book book = BookRepositoryTest.createTestBook();
        Book savedBook = bookRepository.insert(book);
        assertNull(savedBook.getGoogleBookDetails());

        GoogleBooksDaoAsync async = new GoogleBooksDaoAsync(bookRepository);
        async.setBooksGoogleBooksApiGetByIdUrl("https://www.googleapis.com/books/v1/volumes/");
        async.setBooksGoogleBooksApiCountryCode("country=GB");
        async.updateBookWithGoogleBookDetails(savedBook, "mM8qDwAAQBAJ");

        Book updatedBook = bookRepository.
                findById(savedBook.getId()).orElseThrow(() -> new IllegalStateException("Expected book not found"));
        assertNotNull(updatedBook.getGoogleBookDetails(),
                "Google book details in Item object should not be null");
    }
}
