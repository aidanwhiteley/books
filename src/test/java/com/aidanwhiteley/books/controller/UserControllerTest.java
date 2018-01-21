package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.aidanwhiteley.books.controller.config.BasicAuthInsteadOfOauthWebAccess.*;

import java.net.URI;

import static org.junit.Assert.assertEquals;

public class UserControllerTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void getUserDetailsNoAuthentication() {
        ResponseEntity<User> response = testRestTemplate.getForEntity("/secure/api/books", User.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void getUserDetailsWithAuthentication() {
        TestRestTemplate trtWithAuth = testRestTemplate.withBasicAuth(A_USER, PASSWORD);
        ResponseEntity<User> response = trtWithAuth.getForEntity("/secure/api/user", User.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

}
