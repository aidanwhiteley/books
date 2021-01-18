package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService;
import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.Comment;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.domain.User.Role;
import com.aidanwhiteley.books.repository.BookRepositoryTest;
import com.aidanwhiteley.books.util.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ConstantConditions")
class BookSecureControllerTest extends IntegrationTest {

    private static final String GENRE_TOO_LONG = "abcdefghjijklmnopqrstuvwxyz01234567890";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${books.users.max.page.size}")
    private int maxPageSize;

    public static ResponseEntity<Book> updateBook(User user, Book book, String updatedTitle, JwtUtils jwtUtilsLocal, TestRestTemplate testRestTemplateLocal) {
        book.setTitle(updatedTitle);
        String token = jwtUtilsLocal.createTokenForUser(user);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplateLocal);
        HttpEntity<Book> putData = BookControllerTestUtils.getBookHttpEntity(book, token, xsrfToken);
        ResponseEntity<Book> putResponse = testRestTemplateLocal.exchange("/secure/api/books", HttpMethod.PUT, putData,
                Book.class);

        assertEquals(HttpStatus.NO_CONTENT, putResponse.getStatusCode());

        return putResponse;
    }

    @Test
    void createAndDeleteBook() {
        // Create book
        ResponseEntity<Book> response = BookControllerTestUtils.postBookToServer(jwtUtils, testRestTemplate);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // Get location of created book
        String location = response.getHeaders().getLocation().toString();
        assertNotNull("Location of newly created book should have been provided", location);
        String id = location.substring(location.lastIndexOf("/") + 1);

        // Get an admin user and required tokens and then delete the book
        User user = BookControllerTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplate);
        HttpEntity<Book> request = BookControllerTestUtils.getBookHttpEntity(null, token, xsrfToken);
        response = testRestTemplate.exchange("/secure/api/books/" + id, HttpMethod.DELETE, request, Book.class);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        // Now check that the book can no longer be found
        Book deletedBook = testRestTemplate.getForObject(location, Book.class);
        assertNull(deletedBook.getId());
    }

    @Test
    void tryToCreateInvalidBook() {

        // An empty book should fail
        Book emptyBook = new Book();
        User user = BookControllerTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplate);

        HttpEntity<Book> request = BookControllerTestUtils.getBookHttpEntity(emptyBook, token, xsrfToken);
        ResponseEntity<Book> response = testRestTemplate.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        // Create a valid book and then exceed one of the max field sizes
        Book testBook = BookRepositoryTest.createTestBook();
        testBook.setGenre(GENRE_TOO_LONG);
        request = BookControllerTestUtils.getBookHttpEntity(testBook, token, xsrfToken);
        response = testRestTemplate.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void tryToCreateBookWithNoPermissions() {

        Book testBook = BookRepositoryTest.createTestBook();
        HttpEntity<Book> request = new HttpEntity<>(testBook);
        ResponseEntity<Book> response = testRestTemplate.exchange("/secure/api/books",
                HttpMethod.POST,
                request,
                Book.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void tryToCreateBookWithInsufficientPermissions() {

        Book testBook = BookRepositoryTest.createTestBook();

        // Set up user with just the ROLE_USER role
        User user = BookControllerTestUtils.getTestUser();
        user.removeRole(Role.ROLE_ADMIN);
        user.removeRole(Role.ROLE_EDITOR);
        String token = jwtUtils.createTokenForUser(user);

        HttpEntity<Book> putData = BookControllerTestUtils.getBookHttpEntity(testBook, token);
        ResponseEntity<Book> postResponse = testRestTemplate.exchange("/secure/api/books", HttpMethod.POST, putData,
                Book.class);

        assertEquals(HttpStatus.FORBIDDEN, postResponse.getStatusCode());
    }

    @Test
    void updateBook() {

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
        ResponseEntity<Book> putResponse = updateBook(user, book, updatedTitle, this.jwtUtils, this.testRestTemplate);

        headers = response.getHeaders();
        uri = headers.getLocation();

        // And finally check that the book was actually updated
        Book updatedBook = testRestTemplate.getForObject(uri, Book.class);
        assertEquals(updatedBook.getTitle(), updatedTitle);
    }

    @Test
    void tryToUpdateBookWithInsufficientPermissions() {

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
        HttpEntity<Book> putData = BookControllerTestUtils.getBookHttpEntity(book, token);
        ResponseEntity<Book> putResponse = testRestTemplate.exchange("/secure/api/books", HttpMethod.PUT, putData,
                Book.class);

        assertEquals(HttpStatus.FORBIDDEN, putResponse.getStatusCode());
    }

    @Test
    void tryUpdateActionWhenNoCsrfTokenInRequestHeaders() {

        User user = BookControllerTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplate);

        // Check all works OK when xsrf token is supplied
        Book testBook = BookRepositoryTest.createTestBook();
        HttpEntity<Book> request = BookControllerTestUtils.getBookHttpEntity(testBook, token, xsrfToken);
        ResponseEntity<Book> response = testRestTemplate.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // And now check the action is forbidden when no xsrf token is supplied
        request = BookControllerTestUtils.getBookHttpEntity(testBook, token, null);
        response = testRestTemplate.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void addAndRemoveCommentFromBook() {
        // Create Book
        ResponseEntity<Book> response = BookControllerTestUtils.postBookToServer(jwtUtils, testRestTemplate);

        // Get the location of the book POSTed to the server
        HttpHeaders headers = response.getHeaders();
        URI uri = headers.getLocation();

        // Now go and get the Book
        User user = BookControllerTestUtils.getTestUser();
        Book book = testRestTemplate.getForEntity(uri, Book.class).getBody();
        assertEquals(book.getTitle(), BookRepositoryTest.createTestBook().getTitle());
        String bookId = book.getId();

        // Now create a comment
        final String commentText = "A sample comment";
        Comment newComment = new Comment();
        newComment.setCommentText(commentText);

        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplate);
        HttpEntity<Comment> postData = BookControllerTestUtils.getBookHttpEntityForComment(newComment, token, xsrfToken);
        ResponseEntity<Book> postResponse = testRestTemplate.exchange("/secure/api/books/" + bookId + "/comments", HttpMethod.POST, postData,
                Book.class);

        assertEquals(HttpStatus.OK, postResponse.getStatusCode());
        assertEquals(1, postResponse.getBody().getComments().size());
        assertEquals(commentText,postResponse.getBody().getComments().get(0).getCommentText());

        // Now remove the comment (the reuse of the postData is OK as we just need the headers - not the body)
        String commentId = postResponse.getBody().getComments().get(0).getId();
        ResponseEntity<Book> deleteResponse = testRestTemplate.exchange("/secure/api/books/" + bookId + "/comments/" + commentId, HttpMethod.DELETE, postData,
                Book.class);
        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
        assertEquals(1, deleteResponse.getBody().getComments().size());
        assertTrue(deleteResponse.getBody().getComments().get(0).getDeletedBy().contains(user.getFullName()));
        assertTrue(deleteResponse.getBody().getComments().get(0).isDeleted());
    }
    
    @Test
    void findBooksByReader() {
		User user = BookControllerTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplate);
        HttpEntity<Book> request = BookControllerTestUtils.getBookHttpEntity(null, token, xsrfToken);
        
        final String testReader = "Fred Bloggs";
        ResponseEntity<String> response = testRestTemplate.exchange("/secure/api/books?reader=" + testReader, HttpMethod.GET, request, String.class);   
        List<Book> books = JsonPath.read(response.getBody(), "$.content");
        assertTrue(books.size() > 0);

        final String emptyReader = "";
        response = testRestTemplate.exchange("/secure/api/books?reader=" + emptyReader, HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        final int excessivePageSize = maxPageSize + 1000;
        response = testRestTemplate.exchange("/secure/api/books?reader=" + testReader + "&page=0&size=" + excessivePageSize, HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void findBookReaders() {
        User user = BookControllerTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplate);
        HttpEntity<Book> request = BookControllerTestUtils.getBookHttpEntity(null, token, xsrfToken);

        ResponseEntity<String> response = testRestTemplate.exchange("/secure/api/books/readers", HttpMethod.GET, request, String.class);
        List<String> bookReaders = JsonPath.read(response.getBody(), "$[*].reader");
        assertTrue(bookReaders.size() > 0);
    }

    @Test
    void testDebugHeaders() {
        User user = BookControllerTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);

        // Re-using "book related" code to get required headers easily set up
        Book testBook = BookRepositoryTest.createTestBook();
        HttpEntity<Book> request = BookControllerTestUtils.getBookHttpEntity(testBook, token, null);
        ResponseEntity<String> response = testRestTemplate.exchange("/secure/api/debugheaders", HttpMethod.GET, request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains(JwtAuthenticationService.JWT_COOKIE_NAME));
    }

}