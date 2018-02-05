package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.repository.BookRepositoryTest;
import com.aidanwhiteley.books.util.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.net.URI;
import java.util.List;

import static com.aidanwhiteley.books.controller.config.BasicAuthInsteadOfOauthWebAccess.AN_ADMIN;
import static com.aidanwhiteley.books.controller.config.BasicAuthInsteadOfOauthWebAccess.PASSWORD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BookControllerTest extends IntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookControllerTest.class);

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void findBookById() {
        ResponseEntity<Book> response = postBookToServer();
        HttpHeaders headers = response.getHeaders();
        URI uri = headers.getLocation();

        Book book = testRestTemplate.getForObject(uri, Book.class);
        assertEquals(book.getId(), uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1));
    }

    @Test
    public void findByAuthor() {
        postBookToServer();

        ResponseEntity<String> response = testRestTemplate.exchange("/api/books?author=" + BookRepositoryTest.DR_ZEUSS + "&page=0&size=10", HttpMethod.GET,
                null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Returns a "page" of books - so look for the content of the page
        List<Book> books = JsonPath.read(response.getBody(), "$.content");
        LOGGER.debug("Retrieved JSON was: " + response.getBody());

        assertTrue("No books found", books.size() > 0);
    }

    private ResponseEntity<Book> postBookToServer() {
        Book testBook = BookRepositoryTest.createTestBook();
        HttpEntity<Book> request = new HttpEntity<>(testBook);

        TestRestTemplate trtWithAuth = testRestTemplate.withBasicAuth(AN_ADMIN, PASSWORD);

        return trtWithAuth
                .exchange("/secure/api/books", HttpMethod.POST, request, Book.class);
    }

}