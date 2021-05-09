package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.controller.BookControllerTestUtils;
import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService;
import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.User;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActuatorTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${info.build.name}")
    private String projectName;

    @Test
    void checkActuatorEndpointsNotAvailableWithoutActuatorRole() {
        // Re-use existing test class functionality to get a user without the ACTUATOR role
        User user = BookControllerTestUtils.getTestUser();
        ResponseEntity<String> response = getResponseStringEntity(user, "/actuator");
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "User without ROLE_ACTUATOR should be forbidden");
    }

    @Test
    void checkActuatorEndpointsAreAvailableWithActuatorRole() {
        User user = BookControllerTestUtils.getTestUser();
        user.addRole(User.Role.ROLE_ACTUATOR);
        ResponseEntity<String> response = getResponseStringEntity(user, "/actuator");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "User with ROLE_ACTUATOR should be OK");
    }

    @Test
    void checkActuatorEndpointsNotAvailableWithAdminRole() {
        // Re-use existing test class functionality to get a user without the ACTUATOR role
        User user = BookControllerTestUtils.getTestUser();
        user.addRole(User.Role.ROLE_ADMIN);
        ResponseEntity<String> response = getResponseStringEntity(user, "/actuator");
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "User with only ROLE_ADMIN should be forbidden");
    }

    @Test
    void checkExpectedEndpointAvailable() {
        User user = BookControllerTestUtils.getTestUser();
        user.addRole(User.Role.ROLE_ACTUATOR);
        ResponseEntity<String> response = getResponseStringEntity(user, "/actuator/scheduledtasks");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "User with ROLE_ACTUATOR should be able to see scheduledtasks");
    }

    @Test
    void checkUnexpectedEndpointNotAvailable() {
        User user = BookControllerTestUtils.getTestUser();
        user.addRole(User.Role.ROLE_ACTUATOR);
        ResponseEntity<String> response = getResponseStringEntity(user, "/actuator/shutdown");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "No user should see shutdown endpoint");
    }

    @Test
    void checkSampleActuatorValue() {
        User user = BookControllerTestUtils.getTestUser();
        user.addRole(User.Role.ROLE_ACTUATOR);
        ResponseEntity<String> response = getResponseStringEntity(user, "/actuator/info");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be able to get actuator info");

        String actuatorProjectName = JsonPath.read(response.getBody(), "$.build.name");
        assertEquals(this.projectName, actuatorProjectName, "Should to be able see correct project name - set from POM");
    }

    private ResponseEntity<String> getResponseStringEntity(User user, String path) {
        String token = jwtUtils.createTokenForUser(user);
        HttpEntity<String> request = getActuatorHttpEntity(token, null);
        return testRestTemplate.exchange(path, HttpMethod.GET, request, String.class);
    }

    @SuppressWarnings("SameParameterValue")
    private HttpEntity<String> getActuatorHttpEntity(String jwtToken, String xsrfToken) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Cookie", JwtAuthenticationService.JWT_COOKIE_NAME + "=" + jwtToken);
        if (xsrfToken != null && (!xsrfToken.trim().isEmpty())) {
            requestHeaders.add("Cookie", JwtAuthenticationService.XSRF_COOKIE_NAME + "=" + xsrfToken);
            requestHeaders.add(JwtAuthenticationService.XSRF_HEADER_NAME, xsrfToken);
        }

        return new HttpEntity<>(requestHeaders);
    }
}
