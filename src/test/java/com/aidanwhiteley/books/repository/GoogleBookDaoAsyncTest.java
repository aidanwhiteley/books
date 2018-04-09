package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.util.IntegrationTest;
import com.aidanwhiteley.books.util.Stubby4JUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.Assert.assertNull;

@ActiveProfiles({"test", "fongo"})
public class GoogleBookDaoAsyncTest extends IntegrationTest {

    private static final String SPRING_FRAMEWORK_GOOGLE_BOOK_ID = "oMVIzzKjJCcC";

    @Autowired
    BookRepository bookRepository;

    @Autowired
    GoogleBooksDaoAsync async;

    @BeforeClass
    public static void setUpStubby() throws Exception {
        Stubby4JUtil.configureStubServer();
    }

    @AfterClass
    public static void tearDownStubby() throws Exception {
        Stubby4JUtil.stopStubServer();
    }

    @Test
    public void testBookUpdatedWithGoogleBookDetails() {
        Book book = BookRepositoryTest.createTestBook();
        Book savedBook = bookRepository.insert(book);
        assertNull(savedBook.getGoogleBookDetails());

        async.updateBookWithGoogleBookDetails(savedBook, SPRING_FRAMEWORK_GOOGLE_BOOK_ID);

        Book updatedBook = bookRepository.
                findById(savedBook.getId()).orElseThrow(() -> new IllegalStateException("Expected book not found"));
        assertNotNull(updatedBook.getGoogleBookDetails(),
                "Google book details in Item object should not be null");
    }

}
