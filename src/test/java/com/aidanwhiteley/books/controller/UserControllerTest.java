package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UserControllerTest extends IntegrationTest {

    private static final String NO_AUTH_SPRING_PROFILE = "no-auth";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private Environment environment;

    @Value("${books.jwt.actuatorExpiryInMilliSeconds}")
    private long expiryInMilliSecondsActuator;

    @Test
    public void getUserDetailsNoAuthentication() {
        int expectedStatusCode = (Arrays.stream(this.environment.getActiveProfiles()).anyMatch(s -> s.contains(NO_AUTH_SPRING_PROFILE))) ?
            HttpStatus.OK.value() : HttpStatus.UNAUTHORIZED.value();

        ResponseEntity<User> response = testRestTemplate.getForEntity("/secure/api/user", User.class);
        assertEquals(expectedStatusCode, response.getStatusCode().value());
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
        assertNotNull(user);

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
    public void tryToPatchOwnUserid() {
        ResponseEntity<User> response = getUserDetails();
        assertEquals(HttpStatus.OK, response.getStatusCode());

        User aUser = response.getBody();
        assertNotNull(aUser);

        String token = jwtUtils.createTokenForUser(aUser);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplate);
        HttpEntity<User> request = BookControllerTestUtils.getUserHttpEntity(aUser, token, xsrfToken);

        // Shouldnt be able to patch own userid!
        String userPatchResponse = testRestTemplate.patchForObject("/secure/api/users/" + aUser.getId(), request, String.class);
        assertTrue(userPatchResponse.contains(UserController.CANT_CHANGE_PERMISSIONS_FOR_YOUR_OWN_LOGGED_ON_USER));
    }
    
	private ResponseEntity<User> getUserDetails() {
		User user = BookControllerTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplate);
        HttpEntity<Book> request = BookControllerTestUtils.getBookHttpEntity(null, token, xsrfToken);
        return testRestTemplate.exchange("/secure/api/user", HttpMethod.GET, request, User.class);
	}

}
