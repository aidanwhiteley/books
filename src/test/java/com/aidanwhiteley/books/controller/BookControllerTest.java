package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.BookRepositoryTest;
import com.aidanwhiteley.books.repository.dtos.BooksByGenre;
import com.aidanwhiteley.books.repository.dtos.BooksByRating;
import com.aidanwhiteley.books.util.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.*;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static com.aidanwhiteley.books.repository.BookRepositoryTest.J_UNIT_TESTING_FOR_BEGINNERS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ConstantConditions")
public class BookControllerTest extends IntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookControllerTest.class);

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private Environment environment;

    @Value("${books.users.default.page.size}")
    private int defaultPageSize;

    @Test
    public void findBookById() {
        ResponseEntity<Book> response = BookControllerTestUtils.postBookToServer(jwtUtils, testRestTemplate);
        HttpHeaders headers = response.getHeaders();
        URI uri = headers.getLocation();

        Book book = testRestTemplate.getForObject(uri, Book.class);
        assertEquals(book.getId(), uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1));
    }

    @Test
    public void findByAuthor() {
        BookControllerTestUtils.postBookToServer(jwtUtils, testRestTemplate);

        ResponseEntity<String> response = testRestTemplate.exchange("/api/books?author=" + BookRepositoryTest.DR_ZEUSS + "&page=0&size=10", HttpMethod.GET,
                null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Returns a "page" of books - so look for the content of the page
        List<Book> books = JsonPath.read(response.getBody(), "$.content");
        LOGGER.debug("Retrieved JSON was: " + response.getBody());

        assertTrue("No books found", books.size() > 0);
    }

    @Test
    public void testSensitiveDataNotReturnedToAnonymousUser() {
        ResponseEntity<Book> response = BookControllerTestUtils.postBookToServer(jwtUtils, testRestTemplate);
        String location = response.getHeaders().getLocation().toString();
        Book book = testRestTemplate.getForObject(location, Book.class);

        // Title should be available to everyone
        assertEquals(J_UNIT_TESTING_FOR_BEGINNERS, book.getTitle());
        // Email should only be available to admins
        assertEquals("", book.getCreatedBy().getEmail());
    }

    @Test
    public void testSensitiveDataIsReturnedToAdminUser() {
        Book testBook = BookRepositoryTest.createTestBook();
        User user = BookControllerTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplate);
        HttpEntity<Book> request = BookControllerTestUtils.getBookHttpEntity(testBook, token, xsrfToken);

        ResponseEntity<Book> response = testRestTemplate
                .exchange("/secure/api/books", HttpMethod.POST, request, Book.class);

        String location = response.getHeaders().getLocation().toString();
        Book book = testRestTemplate
                .exchange(location, HttpMethod.GET, request, Book.class).getBody();

        // Title should be available to everyone
        assertEquals(J_UNIT_TESTING_FOR_BEGINNERS, book.getTitle());
        // Email should only be available to admins
        assertEquals(BookControllerTestUtils.DUMMY_EMAIL, book.getCreatedBy().getEmail());
    }

    @Test
    public void testUserDataIsReturnedToEditorUser() {
        Book testBook = BookRepositoryTest.createTestBook();
        User user = BookControllerTestUtils.getEditorTestUser();
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplate);

        HttpEntity<Book> request = BookControllerTestUtils.getBookHttpEntity(testBook, token, xsrfToken);

        ResponseEntity<Book> response = testRestTemplate
                .exchange("/secure/api/books", HttpMethod.POST, request, Book.class);

        String location = response.getHeaders().getLocation().toString();
        Book book = testRestTemplate
                .exchange(location, HttpMethod.GET, request, Book.class).getBody();

        // Title should be available to everyone
        assertEquals(J_UNIT_TESTING_FOR_BEGINNERS, book.getTitle());
        // Email should only be available to admins - not editors
        assertEquals("", book.getCreatedBy().getEmail());
        // But the name of the person who created the Book should be available
        assertEquals(BookControllerTestUtils.USER_WITH_EDITOR_ROLE_FULL_NAME, book.getCreatedBy().getFullName());

    }

    @Test
    public void findUsingFullTextSearch() {

        // This test doesnt run with Fongo as it uses weighted full text index
        // against multiple fields - which is not currently supported by Fongo.
        if (Arrays.asList(this.environment.getActiveProfiles()).contains("fongo")) {
            LOGGER.warn("Test skipped - Fongo doesnt yet support weighted full text indexes on multiple fields");
            return;
        }

        BookControllerTestUtils.postBookToServer(jwtUtils, testRestTemplate);

        ResponseEntity<String> response = testRestTemplate.exchange("/api/books?search=" + BookRepositoryTest.DR_ZEUSS + "&page=0&size=10", HttpMethod.GET,
                null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Returns a "page" of books - so look for the content of the page
        List<Book> books = JsonPath.read(response.getBody(), "$.content");
        LOGGER.debug("Retrieved JSON was: " + response.getBody());

        assertTrue("No books found", books.size() >= 1);
    }

    @Test
    public void fullTextSearchShouldntFindStopWord() {

        // This test doesnt run with Fongo as it uses weighted full text index
        // against multiple fields - which is not currently supported by Fongo.
        if (Arrays.asList(this.environment.getActiveProfiles()).contains("fongo")) {
            LOGGER.warn("Test skipped - Fongo doesnt yet support weighted full text indexes on multiple fields");
            return;
        }

        BookControllerTestUtils.postBookToServer(jwtUtils, testRestTemplate);

        // First check that we find the expected data
        final String bookDescription = "A guide to poking software";
        ResponseEntity<String> response = testRestTemplate.exchange("/api/books?search=" + bookDescription + "&page=0&size=10", HttpMethod.GET,
                null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Book> books = JsonPath.read(response.getBody(), "$.content");
        assertTrue("Search didnt find a book", books.size() >= 1);

        // Then check that we dont get a match when using a "stop" work
        final String aStopWord = "A";
        response = testRestTemplate.exchange("/api/books?search=" + aStopWord + "&page=0&size=10", HttpMethod.GET,
                null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        books = JsonPath.read(response.getBody(), "$.content");
        assertEquals("Search unexpectedly found a book", 0, books.size());
    }

    @Test
    public void findAllByWhenEnteredDesc() {
        ResponseEntity<String> response = testRestTemplate.exchange("/api/books", HttpMethod.GET, null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Returns a "page" of books - so look for the content of the page
        List<Book> books = JsonPath.read(response.getBody(), "$.content");
        LOGGER.debug("Retrieved JSON was: " + response.getBody());
        assertEquals("Default page size of books expected", books.size(), defaultPageSize);
    }

    @Test
    public void findBooksByGenre() {
        ResponseEntity<String> response = testRestTemplate.exchange("/api/books?genre=Novel", HttpMethod.GET, null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<Book> books = JsonPath.read(response.getBody(), "$.content");
        LOGGER.debug("Retrieved JSON was: " + response.getBody());
        assertTrue("Expected to find novels", books.size() > 0);
    }

    @Test
    public void testBookDataSummaryApis() {

        // Summary stats
        ResponseEntity<String> response = testRestTemplate.exchange("/api/books/stats", HttpMethod.GET, null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        int count = JsonPath.parse(response.getBody()).read("$.count", Integer.class);
        assertTrue(count > 0);
        List<BooksByGenre> genres = JsonPath.read(response.getBody(), "$.bookByGenre");
        assertTrue(genres.size() > 0);
        List<BooksByRating> ratings = JsonPath.read(response.getBody(), "$.booksByRating");
        assertTrue(ratings.size() > 0);
    }

}