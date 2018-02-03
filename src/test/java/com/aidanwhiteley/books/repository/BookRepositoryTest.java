package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.Owner;
import com.aidanwhiteley.books.util.DummyAuthenticationUtils;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BookRepositoryTest extends IntegrationTest {

    public static final String DR_ZEUSS = "Dr Zuess";
    private static final String J_UNIT_TESTING_FOR_BEGINNERS = "JUnit testing for beginners";
    private static final String A_GUIDE_TO_POKING_SOFTWARE = "A guide to poking software";
    private static final String COMPUTING = "Computing";
    private static final String DESIGN_PATTERNS = "Design Patterns";

    private static final int PAGE = 0;
    private static final int PAGE_SIZE = 10;

    @Autowired
    BookRepository bookRepository;

    public static Book createTestBook() {
        return Book.builder().title(J_UNIT_TESTING_FOR_BEGINNERS)
                .summary(A_GUIDE_TO_POKING_SOFTWARE).genre(COMPUTING)
                .author(DR_ZEUSS).rating(Book.Rating.POOR)
                .entered(LocalDateTime.of(2016, 11, 20, 0, 0))
                .similarTo(DESIGN_PATTERNS)
                .createdBy(new Owner(DummyAuthenticationUtils.getTestUser()))
                .build();
    }

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
        PageRequest pageObj = new PageRequest(PAGE, PAGE_SIZE);
        Page<Book> books = bookRepository.findAllByAuthor(pageObj, DR_ZEUSS);
        assertTrue(books.getContent().size() >= 1);
        assertTrue(books.getContent().get(0).getAuthor().equals(DR_ZEUSS));

        // The book should have a system created id value.
        assertNotNull(books.getContent().get(0).getId());
    }
}