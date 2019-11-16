package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Objects;

import static org.junit.Assert.assertEquals;

@ActiveProfiles({"mongo-java-server-no-auth"})
public class UserControllerNoAuthTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void getUserDetailsNoAuthentication() {
        int expectedStatusCode = HttpStatus.OK.value();

        ResponseEntity<User> response = testRestTemplate.getForEntity("/secure/api/user", User.class);
        assertEquals(expectedStatusCode, response.getStatusCode().value());

        assertEquals(Objects.requireNonNull(response.getBody()).getHighestRole(), User.Role.ROLE_ADMIN);
    }


}
