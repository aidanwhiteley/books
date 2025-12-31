package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.util.BookTestUtils;
import com.aidanwhiteley.books.util.IntegrationTest;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import static org.junit.jupiter.api.Assertions.assertNull;

@Profile({"dev-mongo-java-server", "dev-mongo-java-server-no-auth", "dev-mongodb-no-auth", "dev-mongodb", "ci"})
@EnableWireMock({
        @ConfigureWireMock(
                port = 0)
})
class GoogleBookDaoAsyncTest extends IntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleBookDaoAsyncTest.class);

    private static final String SPRING_FRAMEWORK_GOOGLE_BOOK_ID = "oMVIzzKjJCcC";

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private GoogleBooksDaoAsync async;

    @Test
    void testBookUpdatedWithGoogleBookDetails() {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Wiremock Mappings: {}", WireMock.listAllStubMappings().getMappings());
        }

        Book book = BookTestUtils.createTestBook();
        Book savedBook = bookRepository.insert(book);
        assertNull(savedBook.getGoogleBookDetails());

        // This will result in a call to the Google Books API being mocked by WireMock
        async.updateBookWithGoogleBookDetails(savedBook, SPRING_FRAMEWORK_GOOGLE_BOOK_ID);

        // TODO - fix such that following call will only take place after async update has completed.
        bookRepository.findById(savedBook.getId()).orElseThrow(() -> new IllegalStateException("Expected book not found"));
    }

}
