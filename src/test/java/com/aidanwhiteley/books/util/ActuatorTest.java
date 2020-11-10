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

import static org.junit.Assert.assertEquals;

public class ActuatorTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${info.build.name}")
    private String projectName;

    @Test
    public void checkActuatorEndpointsNotAvailableWithoutActuatorRole() {
        // Re-use existing test class functionality to get a user without the ACTUATOR role
        User user = BookControllerTestUtils.getTestUser();
        ResponseEntity<String> response = getResponseStringEntity(user, "/actuator");
        assertEquals("User without ROLE_ACTUATOR should be forbidden", HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void checkActuatorEndpointsAreAvailableWithActuatorRole() {
        User user = BookControllerTestUtils.getTestUser();
        user.addRole(User.Role.ROLE_ACTUATOR);
        ResponseEntity<String> response = getResponseStringEntity(user, "/actuator");
        assertEquals("User with ROLE_ACTUATOR should be OK", HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void checkActuatorEndpointsNotAvailableWithAdminRole() {
        // Re-use existing test class functionality to get a user without the ACTUATOR role
        User user = BookControllerTestUtils.getTestUser();
        user.addRole(User.Role.ROLE_ADMIN);
        ResponseEntity<String> response = getResponseStringEntity(user, "/actuator");
        assertEquals("User with only ROLE_ADMIN should be forbidden", HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void checkExpectedEndpointAvailable() {
        User user = BookControllerTestUtils.getTestUser();
        user.addRole(User.Role.ROLE_ACTUATOR);
        ResponseEntity<String> response = getResponseStringEntity(user, "/actuator/scheduledtasks");
        assertEquals("User with ROLE_ACTUATOR should be able to see scheduledtasks", HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void checkUnexpectedEndpointNotAvailable() {
        User user = BookControllerTestUtils.getTestUser();
        user.addRole(User.Role.ROLE_ACTUATOR);
        ResponseEntity<String> response = getResponseStringEntity(user, "/actuator/shutdown");
        assertEquals("No user should see shutdown endpoint", HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void checkSampleActuatorValue() {
        User user = BookControllerTestUtils.getTestUser();
        user.addRole(User.Role.ROLE_ACTUATOR);
        ResponseEntity<String> response = getResponseStringEntity(user, "/actuator/info");
        assertEquals("Should be able to get actuator info", HttpStatus.OK, response.getStatusCode());

        String actuatorProjectName = JsonPath.read(response.getBody(), "$.build.name");
        assertEquals("Should to be able see correct project name - set from POM", this.projectName, actuatorProjectName);
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
