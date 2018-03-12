package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.config.WebSecurityConfiguration;
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
import static org.junit.Assert.assertTrue;

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
        ResponseEntity<Book> response = testRestTemplate.exchange("/secure/api/books",
                HttpMethod.POST,
                request,
                Book.class);

        // Spring security will issue a 302 to redirect to the logon page.
        // For GETs this would be automatically followed and the "logon page"
        // responds with a 403 Forbidden.
        // However, POSTs, PUTs etc the client shouldnt automatically follow the
        // 302 redirect. Hence this test looks for the 302.
        // The test is still successful as the client code is
        // prevented (via the 302 to a logon page) from doing what is doesnt have the
        // required permissions to do.
        assertEquals(HttpStatus.FOUND, response.getStatusCode());
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

        // See comments in the tryToCreateBookWithNoPermissions test for why a 302 is expected.
        assertEquals(HttpStatus.FOUND, postResponse.getStatusCode());
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

        // See comments in the tryToCreateBookWithNoPermissions test for why a 302 is expected.
        assertEquals(HttpStatus.FOUND, putResponse.getStatusCode());
    }

    @Test
    public void tryUpdateActionWhenNoCsrfTokenInRequestHeaders() {

        User user = BookControllerTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplate);

        // Check all works OK when xsrf token is supplied
        Book testBook = BookRepositoryTest.createTestBook();
        HttpEntity<Book> request = BookControllerTestUtils.getBookHttpEntity(testBook, user, token, xsrfToken);
        ResponseEntity<Book> response  = testRestTemplate.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // And now check the action is forbidden when no xsrf token is supplied
        request = BookControllerTestUtils.getBookHttpEntity(testBook, user, token, null);
        response  = testRestTemplate.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);

        // In actual fact, what happens is that the request is re-directed to the "logon page", A 403 would have been preferable
        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertTrue(response.getHeaders().getLocation().getPath().equals(WebSecurityConfiguration.API_LOGIN));
    }

}