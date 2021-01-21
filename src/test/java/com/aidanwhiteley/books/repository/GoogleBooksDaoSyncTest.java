package com.aidanwhiteley.books.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import com.aidanwhiteley.books.util.IntegrationTest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

@Profile({"dev-mongo-java-server", "dev-mongo-java-server-no-auth", "dev-mongodb-no-auth", "dev-mongodb", "ci"})
@AutoConfigureWireMock(port=0, httpsPort = 0)
class GoogleBooksDaoSyncTest extends IntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleBooksDaoSyncTest.class);

    private static final String SPRING_FRAMEWORK_GOOGLE_BOOK_ID = "oMVIzzKjJCcC";
    private static final String SLOW_BOOK_ID = "slowbookid";
    private static final String SERVICE_UNAVAILABLE_BOOK_ID = "serviceunavailablebookid";
    private static final String SPRING_BOOK_TITLE = "Professional Java Development with the Spring Framework";
    private static final int NUMBER_OF_BOOKS_IN_SEARCH_RESULTS = 30;

    @Autowired
    private GoogleBooksDaoSync theDao;

    @Test
    void findByTitle() {
        BookSearchResult result = theDao.searchGoogBooksByTitle("Design Patterns");
        assertNotNull(result);
        assertEquals(NUMBER_OF_BOOKS_IN_SEARCH_RESULTS, result.getItems().size());
        assertTrue("Should have found a book id", result.getItems().get(0).getId().length() > 0);
        assertTrue("Should have found a title", result.getItems().get(0).getVolumeInfo().getTitle().length() > 0);
    }

    @Test
    void findByGoogleBookId() {
        Item result = theDao.searchGoogleBooksByGoogleBookId(SPRING_FRAMEWORK_GOOGLE_BOOK_ID);
        assertNotNull(result);
        assertEquals(SPRING_BOOK_TITLE, result.getVolumeInfo().getTitle());
        assertFalse(result.getAccessInfo().isPublicDomain());
        assertNotNull(result.getVolumeInfo().getImageLinks().getThumbnail());
    }

    @Test
    void confirmFindbyBookTimesOut() {

    	// Turn off unwanted logging for read timeout. Prevents JUnit output having unnecessary stack traces etc.
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(GoogleBooksDaoSync.class).setLevel(Level.valueOf("OFF"));

        try {
            theDao.searchGoogleBooksByGoogleBookId(SLOW_BOOK_ID);
            fail("There should have been a timeout on accessing stubbed http service");
        } catch (ResourceAccessException rae) {
            LOGGER.debug("Expected exception caught");
        }

        context.getLogger(GoogleBooksDaoSync.class).setLevel(Level.valueOf("WARN"));
    }

    @Test
    void confirmFindbyBookIdHandlesServiceUnavailable() {

        // Turn off unwanted logging for read timeout. Prevents JUnit output having unnecessary stack traces etc.
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(GoogleBooksDaoSync.class).setLevel(Level.valueOf("OFF"));

        try {
            theDao.searchGoogleBooksByGoogleBookId(SERVICE_UNAVAILABLE_BOOK_ID);
            fail("There should have been a 503 on accessing stubbed http service");
        } catch (HttpServerErrorException hsee) {
            LOGGER.debug("Expected HttpServerErrorException exception caught: " + hsee);
        }

        context.getLogger(GoogleBooksDaoSync.class).setLevel(Level.valueOf("WARN"));
    }

}