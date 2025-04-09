package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService;
import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.Comment;
import com.aidanwhiteley.books.domain.Owner;
import com.aidanwhiteley.books.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BookTestUtils {

    // These values match data in /src/main/resources/sample_data that are autoloaded
    // into the mongo-java-server / Mongo at the start of the tests
    public static final String USER_WITH_ALL_ROLES_FULL_NAME = "Joe Dimagio";
    public static final String USER_WITH_EDITOR_ROLE_FULL_NAME = "Babe Ruth";
    public static final String ANOTHER_USER_WITH_EDITOR_ROLE_FULL_NAME = "Brian Lara";
    public static final String DUMMY_EMAIL = "joe.dimagio@gmail.com";
    public static final String DR_ZEUSS = "Dr Zuess";
    public static final String J_UNIT_TESTING_FOR_BEGINNERS = "JUnit testing for beginners";
    public static final String A_GUIDE_TO_POKING_SOFTWARE = "A guide to poking software";
    public static final String COMPUTING = "Computing";
    private static final String USER_WITH_ALL_ROLES = "107641999401234521888";
    private static final String USER_WITH_EDITOR_ROLE = "1632142143412347";
    private static final String ANOTHER_USER_WITH_EDITOR_ROLE = "111222333444555666";
    private static final User.AuthenticationProvider PROVIDER_ALL_ROLES_USER = User.AuthenticationProvider.GOOGLE;
    private static final User.AuthenticationProvider PROVIDER_EDITOR_USER = User.AuthenticationProvider.FACEBOOK;

    private static final Logger LOGGER = LoggerFactory.getLogger(BookTestUtils.class);

    public static User getTestUser() {
        User user = new User();
        user.setFullName(USER_WITH_ALL_ROLES_FULL_NAME);
        user.setAuthProvider(PROVIDER_ALL_ROLES_USER);
        user.setFirstLogon(LocalDateTime.now());
        user.setLastLogon(LocalDateTime.now());
        user.setEmail(DUMMY_EMAIL);

        user.setAuthenticationServiceId(USER_WITH_ALL_ROLES);
        user.addRole(User.Role.ROLE_USER);
        user.addRole(User.Role.ROLE_EDITOR);
        user.addRole(User.Role.ROLE_ADMIN);
        return user;
    }

    public static User getEditorTestUser() {
        User user = new User();
        user.setFullName(USER_WITH_EDITOR_ROLE_FULL_NAME);
        user.setAuthenticationServiceId(USER_WITH_EDITOR_ROLE);
        user.setAuthProvider(PROVIDER_EDITOR_USER);
        user.addRole(User.Role.ROLE_USER);
        user.addRole(User.Role.ROLE_EDITOR);

        return user;
    }

    public static User getADifferentEditorTestUser() {
        User user = new User();
        user.setFullName(ANOTHER_USER_WITH_EDITOR_ROLE_FULL_NAME);
        user.setAuthenticationServiceId(ANOTHER_USER_WITH_EDITOR_ROLE);
        user.setAuthProvider(PROVIDER_EDITOR_USER);
        user.addRole(User.Role.ROLE_USER);
        user.addRole(User.Role.ROLE_EDITOR);

        return user;
    }

    public static HttpEntity<Book> getBookHttpEntity(Book testBook, String jwtToken, String xsrfToken) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Cookie", JwtAuthenticationService.JWT_COOKIE_NAME + "=" + jwtToken);
        if (xsrfToken != null && (!xsrfToken.trim().isEmpty())) {
            requestHeaders.add("Cookie", JwtAuthenticationService.XSRF_COOKIE_NAME + "=" + xsrfToken);
            requestHeaders.add(JwtAuthenticationService.XSRF_HEADER_NAME, xsrfToken);
        }

        return new HttpEntity<>(testBook, requestHeaders);
    }

    public static HttpEntity<User> getUserHttpEntity(User aUser, String jwtToken, String xsrfToken) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Cookie", JwtAuthenticationService.JWT_COOKIE_NAME + "=" + jwtToken);
        if (xsrfToken != null && (!xsrfToken.trim().isEmpty())) {
            requestHeaders.add("Cookie", JwtAuthenticationService.XSRF_COOKIE_NAME + "=" + xsrfToken);
            requestHeaders.add(JwtAuthenticationService.XSRF_HEADER_NAME, xsrfToken);
        }

        return new HttpEntity<>(aUser, requestHeaders);
    }

    public static HttpEntity<Comment> getBookHttpEntityForComment(Comment testComment, String jwtToken, String xsrfToken) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Cookie", JwtAuthenticationService.JWT_COOKIE_NAME + "=" + jwtToken);
        if (xsrfToken != null && (!xsrfToken.trim().isEmpty())) {
            requestHeaders.add("Cookie", JwtAuthenticationService.XSRF_COOKIE_NAME + "=" + xsrfToken);
            requestHeaders.add(JwtAuthenticationService.XSRF_HEADER_NAME, xsrfToken);
        }

        return new HttpEntity<>(testComment, requestHeaders);
    }

    public static HttpEntity<Book> getBookHttpEntity(Book testBook, String jwtToken) {
        return getBookHttpEntity(testBook, jwtToken, null);
    }

    public static ResponseEntity<Book> postBookToServer(JwtUtils jwtUtils, TestRestTemplate testRestTemplate) {
        String xsrfToken = getXsrfToken(testRestTemplate);

        Book testBook = createTestBook();
        User user = getTestUser();

        String token = jwtUtils.createTokenForUser(user);
        HttpEntity<Book> request = getBookHttpEntity(testBook, token, xsrfToken);
        ResponseEntity<Book> book = testRestTemplate.exchange("/secure/api/books", HttpMethod.POST, request,
                Book.class);

        assertNotNull(book);
        assertEquals(HttpStatus.CREATED, book.getStatusCode());
        LOGGER.debug("postBookToServer posted book to server successfully");

        return book;
    }

    public static String getXsrfToken(TestRestTemplate testRestTemplate) {
        // First we call a GET endpoint to get a required XSRF-TOKEN cookie value
        ResponseEntity<Book> nonExistentBook = testRestTemplate.getForEntity("/api/book/12345678", Book.class);
        HttpHeaders headers = nonExistentBook.getHeaders();
        String cookies = headers.getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(cookies);
        String[] tokenCookies = cookies.split("XSRF-TOKEN=");
        String tokenCookie = tokenCookies[1];
        return tokenCookie.split(";")[0];
    }

    public static Book createTestBook() {
        Owner owner = new Owner(getTestUser());
        return Book.builder().title(J_UNIT_TESTING_FOR_BEGINNERS)
                .summary(A_GUIDE_TO_POKING_SOFTWARE).genre(COMPUTING)
                .author(DR_ZEUSS).rating(Book.Rating.POOR)
                .createdDateTime(LocalDateTime.of(2016, 11, 20, 0, 0))
                .createdBy(owner)
                .build();
    }
}
