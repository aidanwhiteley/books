package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.repository.BookRepositoryTest;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.net.URI;

import static com.aidanwhiteley.books.controller.config.BasicAuthInsteadOfOauthWebAccess.*;
import static org.junit.Assert.assertEquals;

public class BookSecureControllerTest extends IntegrationTest {

    private static final String GENRE_TOO_LONG = "abcdefghjijklmnopqrstuvwxyz01234567890";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void createBook() {
        ResponseEntity<Book> response = postBookToServer();
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void tryToCreateInvalidBook() {

        TestRestTemplate trtWithAuth = testRestTemplate.withBasicAuth(AN_EDITOR, PASSWORD);

        // An empty book should fail
        Book emptyBook = new Book();
        HttpEntity<Book> request = new HttpEntity<>(emptyBook);
        ResponseEntity<Book> response = trtWithAuth.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        // Create a valid book and then exceed one of the max field sizes
        Book testBook = BookRepositoryTest.createTestBook();
        testBook.setGenre(GENRE_TOO_LONG);
        request = new HttpEntity<>(testBook);
        response = trtWithAuth.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void tryToCreateBookWithNoPermissions() {
        TestRestTemplate trtWithAuth = testRestTemplate;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setOutputStreaming(false);
        trtWithAuth.getRestTemplate().setRequestFactory(requestFactory);

        Book testBook = BookRepositoryTest.createTestBook();
        HttpEntity<Book> request = new HttpEntity<>(testBook);
        ResponseEntity<Book> response =  trtWithAuth.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void tryToCreateBookWithInsufficientPermissions() {
        TestRestTemplate trtWithAuth = testRestTemplate.withBasicAuth(A_USER, PASSWORD);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setOutputStreaming(false);
        trtWithAuth.getRestTemplate().setRequestFactory(requestFactory);

        Book testBook = BookRepositoryTest.createTestBook();
        HttpEntity<Book> request = new HttpEntity<>(testBook);
        ResponseEntity<Book> response =  trtWithAuth.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void updateBook() {

        TestRestTemplate trtWithAuth = testRestTemplate.withBasicAuth(AN_EDITOR, PASSWORD);

        ResponseEntity<Book> response = postBookToServer();
        HttpHeaders headers = response.getHeaders();
        URI uri = headers.getLocation();

        Book book = testRestTemplate.getForObject(uri, Book.class);
        final String updatedTitle = "An updated book title";
        book.setTitle(updatedTitle);
        HttpEntity<Book> putData = new HttpEntity<>(book);

        ResponseEntity<Book> putResponse = trtWithAuth.exchange("/secure/api/books", HttpMethod.PUT, putData,
                Book.class);
        assertEquals(putResponse.getStatusCode(), HttpStatus.NO_CONTENT);
        headers = response.getHeaders();
        uri = headers.getLocation();

        Book updatedBook = trtWithAuth.getForObject(uri, Book.class);
        assertEquals(updatedBook.getTitle(), updatedTitle);
    }

    @Test
    public void tryToUpdateBookWithInsufficientPermissions() {
        TestRestTemplate trtWithAuth = testRestTemplate.withBasicAuth(A_USER, PASSWORD);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setOutputStreaming(false);
        trtWithAuth.getRestTemplate().setRequestFactory(requestFactory);

        ResponseEntity<Book> response = postBookToServer();
        HttpHeaders headers = response.getHeaders();
        URI uri = headers.getLocation();

        Book book = testRestTemplate.getForObject(uri, Book.class);
        final String updatedTitle = "An updated book title";
        book.setTitle(updatedTitle);
        HttpEntity<Book> putData = new HttpEntity<>(book);

        ResponseEntity<Book> putResponse = trtWithAuth.exchange("/secure/api/books", HttpMethod.PUT, putData,
                Book.class);

        assertEquals(HttpStatus.FORBIDDEN, putResponse.getStatusCode());
    }

    private ResponseEntity<Book> postBookToServer() {
        Book testBook = BookRepositoryTest.createTestBook();
        HttpEntity<Book> request = new HttpEntity<>(testBook);

        TestRestTemplate trtWithAuth = testRestTemplate.withBasicAuth(AN_EDITOR, PASSWORD);

        return trtWithAuth.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);
    }

}