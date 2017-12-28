package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@IfProfileValue(name = "spring.profiles.active", value = "integration")
public class BookRepositoryTest {

    public static final String J_UNIT_TESTING_FOR_BEGINNERS = "JUnit testing for beginners";
    public static final String A_GUIDE_TO_POKING_SOFTWARE = "A guide to poking software";
    public static final String COMPUTING = "Computing";
    public static final String DR_ZEUSS = "Dr Zuess";

    private static final Logger LOGGER = LoggerFactory.getLogger(BookRepositoryTest.class);

    @Autowired
    BookRepository bookRepository;

    @Before
    public void setUp() {
        bookRepository.insert(createTestBook());
    }

    @After
    public void tearDown() {
        bookRepository.deleteAll();
    }

    @Test
    public void findByAuthor() {
        List<Book> books = bookRepository.findByAuthor(DR_ZEUSS);
        assertTrue(books.size() >= 1);
        assertTrue(books.get(0).getAuthor().equals(DR_ZEUSS));
        assertTrue(books.get(0).getTitle().equals(J_UNIT_TESTING_FOR_BEGINNERS));

        LOGGER.info("The generated id was: " + books.get(0).getId());
    }

    public static Book createTestBook() {
        return Book.builder().title(J_UNIT_TESTING_FOR_BEGINNERS).summary(A_GUIDE_TO_POKING_SOFTWARE).genre(COMPUTING)
                .author(DR_ZEUSS).build();
    }
}