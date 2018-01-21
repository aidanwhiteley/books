package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

public class GoogleBooksDaoTest extends IntegrationTest {

    public static final String SPRING_FRAMEWORK_GOOGLE_BOOK_ID = "oMVIzzKjJCcC";
    public static final String SPRING_BOOK_TITLE = "Professional Java Development with the Spring Framework";

    @Autowired
    GoogleBooksDao theDao;

    @Test
    public void findByTitle() {
        BookSearchResult result = theDao.searchGoogBooksByTitle("Design Patterns");
        assertNotNull(result);
        assertTrue("Should have found result items", result.getItems().size() > 0);
        assertTrue("Should have found a book id", result.getItems().get(0).getId().length() > 0);
        assertTrue("Should have found a title", result.getItems().get(0).getVolumeInfo().getTitle().length() > 0);
    }

    @Test
    public void findByGoogleBookId() {
        Item result = theDao.searchGoogleBooksByGoogleBookId(SPRING_FRAMEWORK_GOOGLE_BOOK_ID);
        assertNotNull(result);
        assertEquals(result.getVolumeInfo().getTitle(), SPRING_BOOK_TITLE);

        assertEquals(result.getAccessInfo().isPublicDomain(), false);

        assertNotNull(result.getVolumeInfo().getImageLinks().getThumbnail());
    }
}