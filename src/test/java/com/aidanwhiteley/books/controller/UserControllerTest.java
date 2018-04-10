package com.aidanwhiteley.books.controller;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.util.IntegrationTest;

public class UserControllerTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    public void getUserDetailsNoAuthentication() {
        ResponseEntity<User> response = testRestTemplate.getForEntity("/secure/api/user", User.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
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
        ResponseEntity<User> userDeleteResponse = testRestTemplate.exchange("/secure/api/users/" + user.getId(), HttpMethod.DELETE, request, User.class);
        assertEquals(HttpStatus.CONFLICT, userDeleteResponse.getStatusCode());
        
        // Delete any old userid (method doesnt complain if id doesnt exist - it is still gone!)
        final String madeUpUserid = "abc123";
        userDeleteResponse = testRestTemplate.exchange("/secure/api/users/" + madeUpUserid, HttpMethod.DELETE, request, User.class);
        assertEquals(HttpStatus.OK, userDeleteResponse.getStatusCode());
        
    }
    
	private ResponseEntity<User> getUserDetails() {
		User user = BookControllerTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookControllerTestUtils.getXsrfToken(testRestTemplate);
        HttpEntity<Book> request = BookControllerTestUtils.getBookHttpEntity(null, token, xsrfToken);
        ResponseEntity<User> response = testRestTemplate.exchange("/secure/api/user", HttpMethod.GET, request, User.class);
		return response;
	}

}
