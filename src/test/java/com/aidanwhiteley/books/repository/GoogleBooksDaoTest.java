package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@IfProfileValue(name = "spring.profiles.active", value = "integration")
public class GoogleBooksDaoTest {

    @Autowired
    GoogleBooksDao theDao;

    @Test
    public void findByTitle() {
        BookSearchResult result = theDao.searchGoogBooksByTitle("Design Patterns");
        assertNotNull(result);
        assertTrue("Should have found result items", result.getItems().size() > 0);
        assertTrue("Should have found a book id", result.getItems().get(0).getId().length() > 0);
        assertTrue("Should have found a title", result.getItems().get(0).getVolumeInfo().getTitle().length() > 0);

        System.out.println(result);
    }
}