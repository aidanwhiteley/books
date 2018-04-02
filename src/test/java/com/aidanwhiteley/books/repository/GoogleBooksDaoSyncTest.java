package com.aidanwhiteley.books.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResourceAccessException;

import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import com.aidanwhiteley.books.util.IntegrationTest;
import com.aidanwhiteley.books.util.Stubby4JUtil;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

@ActiveProfiles("test")
public class GoogleBooksDaoSyncTest extends IntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleBooksDaoSyncTest.class);

    private static final String SPRING_FRAMEWORK_GOOGLE_BOOK_ID = "oMVIzzKjJCcC";
    private static final String SLOW_RESPOND_STUB_BOOK_ID = "slowresponsestub";
    private static final String SPRING_BOOK_TITLE = "Professional Java Development with the Spring Framework";
    private static final int NUMBER_OF_BOOKS_IN_SEARCH_RESULTS = 30;

    @Autowired
    GoogleBooksDaoSync theDao;

    @BeforeClass
    public static void setUpStubby() throws Exception {
        Stubby4JUtil.configureStubServer();
    }

    @AfterClass
    public static void tearDownStubby() throws Exception {
        Stubby4JUtil.stopStubServer();
    }

    @Test
    public void findByTitle() {
        BookSearchResult result = theDao.searchGoogBooksByTitle("Design Patterns");
        assertNotNull(result);
        assertEquals(NUMBER_OF_BOOKS_IN_SEARCH_RESULTS, result.getItems().size());
        assertTrue("Should have found a book id", result.getItems().get(0).getId().length() > 0);
        assertTrue("Should have found a title", result.getItems().get(0).getVolumeInfo().getTitle().length() > 0);
    }

    @Test
    public void findByGoogleBookId() {
        Item result = theDao.searchGoogleBooksByGoogleBookId(SPRING_FRAMEWORK_GOOGLE_BOOK_ID);
        assertNotNull(result);
        assertEquals(SPRING_BOOK_TITLE, result.getVolumeInfo().getTitle());
        assertFalse(result.getAccessInfo().isPublicDomain());
        assertNotNull(result.getVolumeInfo().getImageLinks().getThumbnail());
    }

    @Test
    public void confirmFindbyBookTimesOut() {
    	
    	// Turn off unwanted logging for read timeout
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(GoogleBooksDaoSync.class).setLevel(Level.valueOf("OFF"));

        try {
            theDao.searchGoogleBooksByGoogleBookId(SLOW_RESPOND_STUB_BOOK_ID);
            fail("There should have been a timeout on accessing stubbed http service");
        } catch (ResourceAccessException rae) {
            LOGGER.debug("Expected exception caught");
        }
        
        context.getLogger(GoogleBooksDaoSync.class).setLevel(Level.valueOf("ON"));
    }

}