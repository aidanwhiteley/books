package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService;
import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UserControllerTest extends IntegrationTest {

    private static final String COOKIE_HEADER_NAME = "Cookie";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    public void getUserDetailsNoAuthentication() {
        ResponseEntity<User> response = testRestTemplate.getForEntity("/secure/api/user", User.class);
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    public void getUserDetailsWithAuthentication() {
        ResponseEntity<User> response = getUserDetails();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        //noinspection ConstantConditions
        assertEquals(BookControllerTestUtils.USER_WITH_ALL_ROLES_FULL_NAME, response.getBody().getFullName());
    }
    
    @Test
    public void testLogout() {
        ResponseEntity<User> response = getUserDetails();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
		User user = BookControllerTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplate);
        
        // The Book generic is irrelevant in next call when used in the follow on API call
        HttpEntity<Book> request = BookControllerTestUtils.getBookHttpEntity(null, token, xsrfToken);
        ResponseEntity<User> logoutResponse = testRestTemplate.exchange("/secure/api/logout", HttpMethod.POST, request, User.class);
        assertEquals(HttpStatus.OK, logoutResponse.getStatusCode());
        
        // TODO - check that the cookies have been cleared down
    }
    
    @Test
    public void tryToDeleteUserid() {
    	ResponseEntity<User> response = getUserDetails();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        User user = response.getBody();
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplate);
        HttpEntity<Book> request = BookControllerTestUtils.getBookHttpEntity(null, token, xsrfToken);
        
        // Shouldnt be able to delete own userid!
        assertNotNull(user);
        ResponseEntity<User> userDeleteResponse = testRestTemplate.exchange("/secure/api/users/" + user.getId(), HttpMethod.DELETE, request, User.class);
        assertEquals(HttpStatus.CONFLICT, userDeleteResponse.getStatusCode());
        
        // Delete any old userid (method doesnt complain if id doesnt exist - it is still gone!)
        final String madeUpUserid = "abc123";
        userDeleteResponse = testRestTemplate.exchange("/secure/api/users/" + madeUpUserid, HttpMethod.DELETE, request, User.class);
        assertEquals(HttpStatus.OK, userDeleteResponse.getStatusCode());
        
    }

    @Test
    public void tryToGetActuatorJwtToken() {
        // Get a user with admin access
        User user = BookControllerTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(COOKIE_HEADER_NAME, JwtAuthenticationService.JWT_COOKIE_NAME + "=" + token);
        HttpEntity<String> request = new HttpEntity<>(null, requestHeaders);
        ResponseEntity<String> actuatorJwtToken = testRestTemplate.exchange("/secure/api/users/actuator", HttpMethod.GET, request, String.class);

        User userFromToken = jwtUtils.getUserFromToken(actuatorJwtToken.getBody());
        assertEquals("Call should succeed", HttpStatus.OK, actuatorJwtToken.getStatusCode());
        assertEquals("Actuator user should have single role", 1, userFromToken.getRoles().size());
        assertEquals("Actuator user should only have actuator role", User.Role.ROLE_ACTUATOR, userFromToken.getRoles().get(0));
        assertEquals("Actuator authprovider should be local", User.AuthenticationProvider.LOCAL, userFromToken.getAuthProvider());

        assertEquals("Expiry should be as specified in config", 1000L, jwtUtils.getExpiryFromToken(actuatorJwtToken.getBody()).getTime());

    }

    @Test
    public void tryToGetActuatorJwtTokenWithoutAdminAccess() {
        // Get a user without admin access
        User user = BookControllerTestUtils.getEditorTestUser();
        String token = jwtUtils.createTokenForUser(user);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(COOKIE_HEADER_NAME, JwtAuthenticationService.JWT_COOKIE_NAME + "=" + token);
        HttpEntity<String> request = new HttpEntity<>(null, requestHeaders);
        ResponseEntity<String> actuatorJwtToken = testRestTemplate.exchange("/secure/api/users/actuator", HttpMethod.GET, request, String.class);

        assertEquals("Access should be forbidden", HttpStatus.FORBIDDEN, actuatorJwtToken.getStatusCode());
    }
    
	private ResponseEntity<User> getUserDetails() {
		User user = BookControllerTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplate);
        HttpEntity<Book> request = BookControllerTestUtils.getBookHttpEntity(null, token, xsrfToken);
        return testRestTemplate.exchange("/secure/api/user", HttpMethod.GET, request, User.class);
	}

}
