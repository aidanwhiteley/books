package com.aidanwhiteley.books.controller;

import static com.aidanwhiteley.books.controller.config.BasicAuthInsteadOfOauthWebAccess.AN_EDITOR;
import static com.aidanwhiteley.books.controller.config.BasicAuthInsteadOfOauthWebAccess.A_USER;
import static com.aidanwhiteley.books.controller.config.BasicAuthInsteadOfOauthWebAccess.PASSWORD;
import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService;
import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.domain.User.Role;
import com.aidanwhiteley.books.repository.BookRepositoryTest;
import com.aidanwhiteley.books.util.DummyAuthenticationUtils;
import com.aidanwhiteley.books.util.IntegrationTest;

public class BookSecureControllerTest extends IntegrationTest {

    private static final String GENRE_TOO_LONG = "abcdefghjijklmnopqrstuvwxyz01234567890";

    @Autowired
    private TestRestTemplate testRestTemplate;
    
    @Autowired
    private JwtUtils jwtUtils;

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

        //TestRestTemplate trtWithAuth = testRestTemplate.withBasicAuth(AN_EDITOR, PASSWORD);

        ResponseEntity<Book> response = postBookToServer();
        System.out.println("And OK to 2");
        HttpHeaders headers = response.getHeaders();
        System.out.println("Headers: " + headers.toString());
        URI uri = headers.getLocation();
        
        ////////
        User user = DummyAuthenticationUtils.getTestUser();
        user.addRole(Role.ROLE_EDITOR);
        
        //////
        Date expdate= new Date();
        expdate.setTime (expdate.getTime() + (3600 * 1000));
        DateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        ////
        
        HttpHeaders requestHeaders = new HttpHeaders();
        String token = jwtUtils.createTokenForUser(user);
        requestHeaders.add("Cookie", JwtAuthenticationService.JWT_COOKIE_NAME + "=" + token + ";Expires=" + df.format(expdate) + ";path=/");
        System.out.println("And OK to 3");
        //HttpEntity<Book> request = new HttpEntity<>(testBook, requestHeaders);
        ////////
        
        Book book = null;
        try {
        	System.out.println("URI is: " + uri.toString());
        	book = testRestTemplate.getForObject(uri, Book.class);
        } catch (Exception e) {
        	System.out.println("Exception is : " + e);
        	e.printStackTrace();
        }
        System.out.println("And OK to 4");
        final String updatedTitle = "An updated book title";
        book.setTitle(updatedTitle);
        HttpEntity<Book> putData = new HttpEntity<>(book, requestHeaders);

        ResponseEntity<Book> putResponse = testRestTemplate.exchange("/secure/api/books", HttpMethod.PUT, putData,
                Book.class);
        assertEquals(putResponse.getStatusCode(), HttpStatus.NO_CONTENT);
        headers = response.getHeaders();
        uri = headers.getLocation();

        Book updatedBook = testRestTemplate.getForObject(uri, Book.class);
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
        User user = DummyAuthenticationUtils.getTestUser();
        user.addRole(Role.ROLE_EDITOR);
        
        HttpHeaders requestHeaders = new HttpHeaders();
        String token = jwtUtils.createTokenForUser(user);
        
        Date expdate= new Date();
        expdate.setTime (expdate.getTime() + (3600 * 1000));
        DateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        
        requestHeaders.add(HttpHeaders.COOKIE, JwtAuthenticationService.JWT_COOKIE_NAME + "=" + token + ";Expires=" + df.format(expdate) + ";path=/");
        HttpEntity<Book> request = new HttpEntity<>(testBook, requestHeaders);

        System.out.println("Ok to here..");
        return testRestTemplate.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);
    }

}