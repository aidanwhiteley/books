package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@IfProfileValue(name = "spring.profiles.active", value = "integration")
public class GoogleBooksDaoTest {

    @Autowired
    GoogleBooksDao theDao;

    public static final String SPRING_FRAMEWORK_GOOGLE_BOOK_ID = "oMVIzzKjJCcC";
    public static final String SPRING_BOOK_TITLE = "Professional Java Development with the Spring Framework";

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
    }
}