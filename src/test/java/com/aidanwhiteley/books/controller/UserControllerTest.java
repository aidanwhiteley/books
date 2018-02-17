package com.aidanwhiteley.books.controller;

import static com.aidanwhiteley.books.controller.config.BasicAuthInsteadOfOauthWebAccess.A_USER;
import static com.aidanwhiteley.books.controller.config.BasicAuthInsteadOfOauthWebAccess.PASSWORD;
import static org.junit.Assert.assertEquals;

import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService;
import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.util.IntegrationTest;

public class UserControllerTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    public void getUserDetailsNoAuthentication() {
        ResponseEntity<User> response = testRestTemplate.getForEntity("/secure/api/books", User.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void getUserDetailsWithAuthentication() {
        User user = BookControllerTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Cookie", JwtAuthenticationService.JWT_COOKIE_NAME + "=" + token);
        HttpEntity<User> entity =  new HttpEntity<>(null, requestHeaders);

        ResponseEntity<User> response = testRestTemplate.exchange("/secure/api/user", HttpMethod.POST, entity, User.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(response.getBody().getFullName(), BookControllerTestUtils.USER_WITH_ALL_ROLES_FULL_NAME);
    }

}
