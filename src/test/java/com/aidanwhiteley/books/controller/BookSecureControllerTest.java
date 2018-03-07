package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.domain.User.Role;
import com.aidanwhiteley.books.repository.BookRepositoryTest;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.net.URI;

import static org.junit.Assert.assertEquals;

public class BookSecureControllerTest extends IntegrationTest {

    private static final String GENRE_TOO_LONG = "abcdefghjijklmnopqrstuvwxyz01234567890";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    public void createBook() {
        ResponseEntity<Book> response = BookControllerTestUtils.postBookToServer(jwtUtils, testRestTemplate);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void tryToCreateInvalidBook() {

        // An empty book should fail
        Book emptyBook = new Book();
        User user = BookControllerTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplate);

        HttpEntity<Book> request = BookControllerTestUtils.getBookHttpEntity(emptyBook, user, token, xsrfToken);
        ResponseEntity<Book> response = testRestTemplate.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        // Create a valid book and then exceed one of the max field sizes
        Book testBook = BookRepositoryTest.createTestBook();
        testBook.setGenre(GENRE_TOO_LONG);
        request = BookControllerTestUtils.getBookHttpEntity(testBook, user, token, xsrfToken);
        response = testRestTemplate.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void tryToCreateBookWithNoPermissions() {

        Book testBook = BookRepositoryTest.createTestBook();
        HttpEntity<Book> request = new HttpEntity<>(testBook);
        ResponseEntity<Book> response = testRestTemplate.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void tryToCreateBookWithInsufficientPermissions() {

        Book testBook = BookRepositoryTest.createTestBook();

        // Set up user with just the ROLE_USER role
        User user = BookControllerTestUtils.getTestUser();
        user.removeRole(Role.ROLE_ADMIN);
        user.removeRole(Role.ROLE_EDITOR);
        String token = jwtUtils.createTokenForUser(user);

        HttpEntity<Book> putData = BookControllerTestUtils.getBookHttpEntity(testBook, user, token);
        ResponseEntity<Book> postResponse = testRestTemplate.exchange("/secure/api/books", HttpMethod.POST, putData,
                Book.class);
        assertEquals(HttpStatus.FORBIDDEN, postResponse.getStatusCode());
    }

    @Test
    public void updateBook() {

        // Create Book
        ResponseEntity<Book> response = BookControllerTestUtils.postBookToServer(jwtUtils, testRestTemplate);

        // Get the location of the book POSTed to the server
        HttpHeaders headers = response.getHeaders();
        URI uri = headers.getLocation();

        // Now go and get the Book
        User user = BookControllerTestUtils.getTestUser();
        Book book = testRestTemplate.getForEntity(uri, Book.class).getBody();
        assertEquals(book.getTitle(), BookRepositoryTest.createTestBook().getTitle());

        // Now update the book - need to supply a JWT / logon token to perform update.
        final String updatedTitle = "An updated book title";
        book.setTitle(updatedTitle);
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplate);
        HttpEntity<Book> putData = BookControllerTestUtils.getBookHttpEntity(book, user, token, xsrfToken);
        ResponseEntity<Book> putResponse = testRestTemplate.exchange("/secure/api/books", HttpMethod.PUT, putData,
                Book.class);

        assertEquals(HttpStatus.NO_CONTENT, putResponse.getStatusCode());
        headers = response.getHeaders();
        uri = headers.getLocation();

        // And finally check that the book was actually updated
        Book updatedBook = testRestTemplate.getForObject(uri, Book.class);
        assertEquals(updatedBook.getTitle(), updatedTitle);
    }

    @Test
    public void tryToUpdateBookWithInsufficientPermissions() {

        ResponseEntity<Book> response = BookControllerTestUtils.postBookToServer(jwtUtils, testRestTemplate);
        HttpHeaders headers = response.getHeaders();
        URI uri = headers.getLocation();
        Book book = testRestTemplate.getForObject(uri, Book.class);

        // Set up user with just the ROLE_USER role
        User user = BookControllerTestUtils.getTestUser();
        user.removeRole(Role.ROLE_ADMIN);
        user.removeRole(Role.ROLE_EDITOR);

        final String updatedTitle = "An updated book title";
        book.setTitle(updatedTitle);
        String token = jwtUtils.createTokenForUser(user);
        HttpEntity<Book> putData = BookControllerTestUtils.getBookHttpEntity(book, user, token);
        ResponseEntity<Book> putResponse = testRestTemplate.exchange("/secure/api/books", HttpMethod.PUT, putData,
                Book.class);

        assertEquals(HttpStatus.FORBIDDEN, putResponse.getStatusCode());
    }

}