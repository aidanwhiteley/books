package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.controller.BookControllerTestUtils;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.Comment;
import com.aidanwhiteley.books.domain.Owner;
import com.aidanwhiteley.books.repository.dtos.BooksByAuthor;
import com.aidanwhiteley.books.repository.dtos.BooksByGenre;
import com.aidanwhiteley.books.repository.dtos.BooksByRating;
import com.aidanwhiteley.books.repository.dtos.BooksByReader;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;

public class BookRepositoryTest extends IntegrationTest {

    public static final String DR_ZEUSS = "Dr Zuess";
    public static final String J_UNIT_TESTING_FOR_BEGINNERS = "JUnit testing for beginners";
    private static final String A_GUIDE_TO_POKING_SOFTWARE = "A guide to poking software";
    private static final String COMPUTING = "Computing";

    private static final String A_COMMENT = "Comments can be tested";
    private static final String ANOTHER_COMMENT = "Especially when there is more than one of them";
    private static final String COMMENT_REMOVER = "Ilie Nastasie";

    private static final int PAGE = 0;
    private static final int PAGE_SIZE = 10;

    @Autowired
    BookRepository bookRepository;

    public static Book createTestBook() {
        return Book.builder().title(J_UNIT_TESTING_FOR_BEGINNERS)
                .summary(A_GUIDE_TO_POKING_SOFTWARE).genre(COMPUTING)
                .author(DR_ZEUSS).rating(Book.Rating.POOR)
                .entered(LocalDateTime.of(2016, 11, 20, 0, 0))
                .createdBy(new Owner(BookControllerTestUtils.getTestUser()))
                .build();
    }

    @Before
    public void setUp() {
        bookRepository.insert(createTestBook());
    }

    @Test
    public void findByAuthor() {
        PageRequest pageObj = PageRequest.of(PAGE, PAGE_SIZE);
        Page<Book> books = bookRepository.findAllByAuthorOrderByEnteredDesc(pageObj, DR_ZEUSS);
        assertTrue(books.getContent().size() >= 1);
        assertEquals(DR_ZEUSS, books.getContent().get(0).getAuthor());

        // The book should have a system created id value.
        assertNotNull(books.getContent().get(0).getId());
    }

    @Test
    public void countBooksByGenre() {
        List<BooksByGenre> list = bookRepository.countBooksByGenre();
        assertTrue(list.size() > 0);
        assertTrue(list.get(0).getGenre().length() > 0);
        assertTrue(list.get(0).getCountOfBooks() > 0);
    }

    @Test
    public void countBooksByRating() {
        List<BooksByRating> list = bookRepository.countBooksByRating();
        assertTrue(list.size() > 0);
        assertTrue(list.get(0).getCountOfBooks() > 0);
    }

    @Test
    public void countBooksByAuthor() {
        List<BooksByAuthor> list = bookRepository.countBooksByAuthor();
        assertTrue(list.size() > 0);
        assertTrue(list.get(0).getCountOfBooks() > 0);
    }

    @Test
    public void countBooksByReader() {
        List<BooksByReader> list = bookRepository.countBooksByReader();
        assertTrue(list.size() > 0);
        assertTrue(list.get(0).getCountOfBooks() > 0);
    }

    @Test
    public void addCommentToBook() {
        Book book = createTestBook();

        Book savedBook = bookRepository.insert(book);

        Comment comment = new Comment(A_COMMENT, new Owner());

        // Returned book holds just the Book's comments - no other data other than the book id.
        Book updatedBook = bookRepository.addCommentToBook(savedBook.getId(), comment);

        assertEquals(1, updatedBook.getComments().size());
        assertEquals(A_COMMENT, updatedBook.getComments().get(0).getCommentText());
    }

    @Test
    public void removeCommentFromBook() {

        // Set up a couple of comments
        Book book = createTestBook();
        Book savedBook = bookRepository.insert(book);
        Comment comment = new Comment(A_COMMENT, new Owner());
        bookRepository.addCommentToBook(savedBook.getId(), comment);
        comment = new Comment(ANOTHER_COMMENT, new Owner());
        bookRepository.addCommentToBook(savedBook.getId(), comment);
        //noinspection ConstantConditions
        Book updatedBook = bookRepository.findById(savedBook.getId()).get();
        assertEquals(2, updatedBook.getComments().size());

        // Returned Book holds just the updated comments
        updatedBook = bookRepository.removeCommentFromBook(savedBook.getId(),
                updatedBook.getComments().get(0).getId(), COMMENT_REMOVER);

        // There should still be two comments but the first should now be "marked" as deleted
        assertEquals(2, updatedBook.getComments().size());
        assertEquals("", updatedBook.getComments().get(0).getCommentText());
        assertTrue(updatedBook.getComments().get(0).isDeleted());
        assertEquals(COMMENT_REMOVER, updatedBook.getComments().get(0).getDeletedBy());
    }
}