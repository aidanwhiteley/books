package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

// See https://github.com/spring-projects/spring-boot/issues/19788 for why we use the syntax below to set the active profile
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.profiles.active=dev-mongo-java-server-no-auth", "books.client.enableCORS=false"})
class UserControllerNoAuthTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    void getUserDetailsNoAuthentication() {
        int expectedStatusCode = HttpStatus.OK.value();

        ResponseEntity<User> response = testRestTemplate.getForEntity("/secure/api/user", User.class);
        assertEquals(expectedStatusCode, response.getStatusCode().value());

        assertEquals(User.Role.ROLE_ADMIN, Objects.requireNonNull(response.getBody()).getHighestRole());
    }


}
