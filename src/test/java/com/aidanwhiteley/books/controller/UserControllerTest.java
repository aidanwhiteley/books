package com.aidanwhiteley.books.controller;

import static com.aidanwhiteley.books.controller.config.BasicAuthInsteadOfOauthWebAccess.A_USER;
import static com.aidanwhiteley.books.controller.config.BasicAuthInsteadOfOauthWebAccess.PASSWORD;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.util.IntegrationTest;

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
