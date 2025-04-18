package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.dtos.BooksByGenre;
import com.aidanwhiteley.books.repository.dtos.BooksByRating;
import com.aidanwhiteley.books.util.BookTestUtils;
import com.aidanwhiteley.books.util.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
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

import static com.aidanwhiteley.books.controller.BookController.PAGE_REQUEST_TOO_BIG_MESSAGE;
import static com.aidanwhiteley.books.util.BookTestUtils.J_UNIT_TESTING_FOR_BEGINNERS;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ConstantConditions")
public class BookControllerTest extends IntegrationTest {

    public static final String IN_MEMORY_MONGODB_SPRING_PROFILE = "mongo-java-server";
    private static final String NO_AUTH_SPRING_PROFILE = "no-auth";
    private static final Logger LOGGER = LoggerFactory.getLogger(BookControllerTest.class);
    private static final String ERROR_MESSAGE_FOR_INVALID_RATING = "Supplied rating parameter not recognised";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private Environment environment;

    @Value("${books.users.default.page.size}")
    private int defaultPageSize;

    @Value("${books.users.max.page.size}")
    private int maxPageSize;

    @Test
    void findBookById() {
        ResponseEntity<Book> response = BookTestUtils.postBookToServer(jwtUtils, testRestTemplate);
        HttpHeaders headers = response.getHeaders();
        URI uri = headers.getLocation();

        Book book = testRestTemplate.getForObject(uri, Book.class);
        assertEquals(book.getId(), uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1));
    }

    @Test
    void findByAuthor() {
        BookTestUtils.postBookToServer(jwtUtils, testRestTemplate);

        ResponseEntity<String> response = testRestTemplate.exchange("/api/books?author=" + BookTestUtils.DR_ZEUSS + "&page=0&size=10", HttpMethod.GET,
                null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Returns a "page" of books - so look for the content of the page
        List<Book> books = JsonPath.read(response.getBody(), "$.content");
        LOGGER.debug("Retrieved JSON was: {}", response.getBody());

        assertFalse(books.isEmpty(), "No books found");
    }

    @Test
    void testSensitiveDataNotReturnedToAnonymousUser() {
        ResponseEntity<Book> response = BookTestUtils.postBookToServer(jwtUtils, testRestTemplate);
        String location = response.getHeaders().getLocation().toString();
        Book book = testRestTemplate.getForObject(location, Book.class);

        // Title should be available to everyone
        assertEquals(J_UNIT_TESTING_FOR_BEGINNERS, book.getTitle());

        // Email should only be available to admins - check not running a profile where users are "auto logged on"
        boolean noAuthProfile = Arrays.stream(this.environment.getActiveProfiles()).
                anyMatch(s -> s.contains(NO_AUTH_SPRING_PROFILE));

        if (!noAuthProfile) {
            String expected = "";
            assertEquals(expected, book.getCreatedBy().getEmail());
        }

        // Now update the book and check that details about who updated the book are not returned
        String updatedTitle = "An updated title";
        User user = BookTestUtils.getTestUser();
        BookSecureControllerTest.updateBook(user, book, updatedTitle, this.jwtUtils, this.testRestTemplate);

        // Check that the book was actually updated
        Book updatedBook = testRestTemplate.getForObject(location, Book.class);
        assertEquals(updatedTitle, updatedBook.getTitle());

        if (!noAuthProfile) {
            // and check that details about who did the update arent returned
            assertTrue(updatedBook.getLastModifiedBy().getLastName().isEmpty());
        }
    }

    @Test
    void testSensitiveDataIsReturnedToAdminUser() {
        Book testBook = BookTestUtils.createTestBook();
        User user = BookTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookTestUtils.getXsrfToken(testRestTemplate);
        HttpEntity<Book> request = BookTestUtils.getBookHttpEntity(testBook, token, xsrfToken);

        ResponseEntity<Book> response = testRestTemplate
                .exchange("/secure/api/books", HttpMethod.POST, request, Book.class);

        String location = response.getHeaders().getLocation().toString();
        Book book = testRestTemplate
                .exchange(location, HttpMethod.GET, request, Book.class).getBody();

        // Title should be available to everyone
        assertEquals(J_UNIT_TESTING_FOR_BEGINNERS, book.getTitle());
        // Email should only be available to admins
        assertEquals(BookTestUtils.DUMMY_EMAIL, book.getCreatedBy().getEmail());


        // Now update the book and check that details about who updated the book are returned to an authorised user
        String updatedTitle = "Another updated title";
        BookSecureControllerTest.updateBook(user, book, updatedTitle, this.jwtUtils, this.testRestTemplate);

        Book updatedBook = testRestTemplate
                .exchange(location, HttpMethod.GET, request, Book.class).getBody();
        assertEquals(updatedTitle, updatedBook.getTitle());
        // Check that details about who did the update ARE returned
        assertEquals(updatedBook.getLastModifiedBy().getFullName(), user.getFullName());
    }

    @Test
    void testUserDataIsReturnedToEditorUser() {
        Book testBook = BookTestUtils.createTestBook();
        User user = BookTestUtils.getEditorTestUser();
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookTestUtils.getXsrfToken(testRestTemplate);

        HttpEntity<Book> request = BookTestUtils.getBookHttpEntity(testBook, token, xsrfToken);

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
        assertEquals(BookTestUtils.USER_WITH_EDITOR_ROLE_FULL_NAME, book.getCreatedBy().getFullName());

    }

    @Test
    void findUsingFullTextSearch() {

        // This test doesn't run with mongo-java-server as it uses weighted full text index
        // against multiple fields - which is not currently supported by mongo-java-server.
        if (Arrays.stream(this.environment.getActiveProfiles()).anyMatch(s -> s.contains(IN_MEMORY_MONGODB_SPRING_PROFILE))) {
            LOGGER.warn("Test skipped - mongo-java-server doesnt yet support weighted full text indexes on multiple fields");
            return;
        }

        BookTestUtils.postBookToServer(jwtUtils, testRestTemplate);

        ResponseEntity<String> response = testRestTemplate.exchange("/api/books?search=" + BookTestUtils.DR_ZEUSS + "&page=0&size=10", HttpMethod.GET,
                null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Returns a "page" of books - so look for the content of the page
        List<Book> books = JsonPath.read(response.getBody(), "$.content");
        LOGGER.debug("Retrieved JSON was: {}", response.getBody());


        assertFalse(books.isEmpty(), "No books found");
    }

    @Test
    void fullTextSearchShouldntFindStopWord() {

        // This test doesnt run with mongo-java-server as it uses weighted full text index
        // against multiple fields - which is not currently supported by mongo-java-server.
        if (Arrays.stream(this.environment.getActiveProfiles()).anyMatch(s -> s.contains(IN_MEMORY_MONGODB_SPRING_PROFILE))) {
            LOGGER.warn("Test skipped - mongo-java-server doesnt yet support weighted full text indexes on multiple fields");
            return;
        }

        BookTestUtils.postBookToServer(jwtUtils, testRestTemplate);

        // First check that we find the expected data
        final String bookDescription = "A guide to poking software";
        ResponseEntity<String> response = testRestTemplate.exchange("/api/books?search=" + bookDescription + "&page=0&size=10", HttpMethod.GET,
                null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Book> books = JsonPath.read(response.getBody(), "$.content");
        assertFalse(books.isEmpty(), "Search didnt find a book");

        // Then check that we dont get a match when using a "stop" work
        final String aStopWord = "A";
        response = testRestTemplate.exchange("/api/books?search=" + aStopWord + "&page=0&size=10", HttpMethod.GET,
                null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        books = JsonPath.read(response.getBody(), "$.content");
        assertEquals(0, books.size(), "Search unexpectedly found a book");
    }

    @Test
    void findAllByWhenCreatedDateTimeDesc() {
        ResponseEntity<String> response = testRestTemplate.exchange("/api/books", HttpMethod.GET, null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Returns a "page" of books - so look for the content of the page
        List<Book> books = JsonPath.read(response.getBody(), "$.content");
        LOGGER.debug("Retrieved JSON was: {}", response.getBody());
        assertEquals(books.size(), defaultPageSize, "Default page size of books expected");
    }

    @Test
    void findBooksByGenre() {
        ResponseEntity<String> response = testRestTemplate.exchange("/api/books?genre=Novel", HttpMethod.GET, null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<Book> books = JsonPath.read(response.getBody(), "$.content");
        LOGGER.debug("Retrieved JSON was: {}", response.getBody());
        assertFalse(books.isEmpty(), "Expected to find novels");
    }

    @Test
    void testBookDataSummaryApis() {

        // Summary stats
        ResponseEntity<String> response = testRestTemplate.exchange("/api/books/stats", HttpMethod.GET, null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        int count = JsonPath.parse(response.getBody()).read("$.count", Integer.class);
        assertTrue(count > 0, "Should find more than 0 books");
        List<BooksByGenre> genres = JsonPath.read(response.getBody(), "$.bookByGenre");
        assertFalse(genres.isEmpty(), "Should find more than 0 genres");
        List<BooksByRating> ratings = JsonPath.read(response.getBody(), "$.booksByRating");
        assertFalse(ratings.isEmpty(), "Should have found more than 0 ratings");
    }

    @Test
    void findBookByRating() {
        ResponseEntity<String> response = testRestTemplate.exchange("/api/books/?rating=GREAT&page=1&size=2", HttpMethod.GET, null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<Book> booksByRating = JsonPath.read(response.getBody(), "$.content");
        LOGGER.debug("Retrieved JSON was: {}", response.getBody());
        assertFalse(booksByRating.isEmpty(), "Expected to find novels");
        assertEquals(2, booksByRating.size(), "Expected to find a page of 2 novels");

        // Test defaults
        response = testRestTemplate.exchange("/api/books/?rating=GREAT", HttpMethod.GET, null, String.class);
        booksByRating = JsonPath.read(response.getBody(), "$.content");
        assertEquals(defaultPageSize, booksByRating.size(), "Expected to find default page size of novels");
    }

    @Test
    void findBookByRatingPreConditions() {
        ResponseEntity<String> response = testRestTemplate.exchange("/api/books/?rating=invalid=1&size=2", HttpMethod.GET, null, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        String bodyContent = response.getBody();
        LOGGER.debug("Retrieved JSON was: {}", bodyContent);
        assertTrue(bodyContent.contains(ERROR_MESSAGE_FOR_INVALID_RATING), "Expected to find a specified error message");
    }

    @Test
    void askForTooManyDataItems() {
        final int tooBig = maxPageSize + 1;

        ResponseEntity<String> response = testRestTemplate.exchange("/api/books/?author=someone&page=0&size=" + tooBig, HttpMethod.GET, null, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.toString().contains(PAGE_REQUEST_TOO_BIG_MESSAGE.formatted(maxPageSize)));

        response = testRestTemplate.exchange("/api/books/?search=something&page=0&size=" + tooBig, HttpMethod.GET, null, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.toString().contains(PAGE_REQUEST_TOO_BIG_MESSAGE.formatted(maxPageSize)));

        response = testRestTemplate.exchange("/api/books/?genre=somegenre&page=0&size=" + tooBig, HttpMethod.GET, null, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.toString().contains(PAGE_REQUEST_TOO_BIG_MESSAGE.formatted(maxPageSize)));

        response = testRestTemplate.exchange("/api/books/?rating=great&page=0&size=" + tooBig, HttpMethod.GET, null, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.toString().contains(PAGE_REQUEST_TOO_BIG_MESSAGE.formatted(maxPageSize)));
    }

    @Test
    void askForEmptySearchCriteria() {
        final String partialErrorMsg = "cannot be empty";

        ResponseEntity<String> response = testRestTemplate.exchange("/api/books/?author=&page=0&size=" + maxPageSize, HttpMethod.GET, null, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.toString().contains(partialErrorMsg));

        response = testRestTemplate.exchange("/api/books/?search=&page=0&size=" + maxPageSize, HttpMethod.GET, null, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.toString().contains(partialErrorMsg));

        response = testRestTemplate.exchange("/api/books/?genre=&page=0&size=" + maxPageSize, HttpMethod.GET, null, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.toString().contains(partialErrorMsg));

        response = testRestTemplate.exchange("/api/books/?rating=&page=0&size=" + maxPageSize, HttpMethod.GET, null, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.toString().contains(partialErrorMsg));
    }

}