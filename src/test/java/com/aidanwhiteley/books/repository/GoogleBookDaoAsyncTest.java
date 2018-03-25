package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class GoogleBookDaoAsyncTest extends IntegrationTest {

    @Autowired
    BookRepository bookRepository;

    @Test
    public void testBookUpdatedWithGoogleBookDetails() {
        GoogleBooksDaoAsync async = new GoogleBooksDaoAsync(bookRepository);
        async.setBooksGoogleBooksApiGetByIdUrl("https://www.googleapis.com/books/v1/volumes/");
        async.setBooksGoogleBooksApiCountryCode("country=GB");

        async.updateBookWithGoogleBookDetails(new Book(), "mM8qDwAAQBAJ");
    }
}
