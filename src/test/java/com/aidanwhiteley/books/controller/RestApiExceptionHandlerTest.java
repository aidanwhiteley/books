package com.aidanwhiteley.books.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.aidanwhiteley.books.controller.dtos.ApiExceptionData;
import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.util.BookTestUtils;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.aidanwhiteley.books.controller.RestApiExceptionHandler.MESSAGE_ILLEGAL_ARGUMENT;
import static com.aidanwhiteley.books.controller.RestApiExceptionHandler.MESSAGE_NOT_FOUND;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc
class RestApiExceptionHandlerTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    void testExceptionHandlerForResourceNotFound() throws Exception {
        RequestBuilder requestBuilder = getGetRequestBuilder("/api/books/987654321");
        mockMvc.perform(requestBuilder)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(NOT_FOUND.value())))
                .andExpect(jsonPath("$.message", containsString(MESSAGE_NOT_FOUND)));
    }

    @Test
    void testExceptionHandlerIllegalArguments() throws Exception {
        RequestBuilder requestBuilder = getGetRequestBuilder("/api/books/?rating=wibble");
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", containsString(MESSAGE_ILLEGAL_ARGUMENT)));
    }

    @Test
    void testExceptionHandlerForNoPermissions() throws Exception {
        Book book = new Book();
        RequestBuilder requestBuilder = getPostRequestBuilder("/secure/api/books", book);
        mockMvc.perform(requestBuilder)
                .andExpect(status().isForbidden());
    }

    @Test
    void testHandleDefaultExceptions() {
        RestApiExceptionHandler raeh = new RestApiExceptionHandler();
        final String errMsg = "Its all gone Pete Tong";
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(RestApiExceptionHandler.class).setLevel(Level.valueOf("OFF"));
        ApiExceptionData aed = raeh.handleDefaultExceptions(new RuntimeException(errMsg), null);
        context.getLogger(RestApiExceptionHandler.class).setLevel(Level.valueOf("ON"));
        // We don't want to accidentally expose any internal implementation details so
        // we don't want the text of the unexpected exception sent to the client.
        assertFalse(aed.getMessage().contains(errMsg));
    }

    private RequestBuilder getGetRequestBuilder(String url) {
        return MockMvcRequestBuilders
                .get(url)
                .accept(MediaType.APPLICATION_JSON);
    }

    @SuppressWarnings("SameParameterValue")
    private RequestBuilder getPostRequestBuilder(String url, Book book) {
        User user = BookTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);
        String xsrfToken = BookTestUtils.getXsrfToken(testRestTemplate);

        HttpEntity<Book> entity = BookTestUtils.getBookHttpEntity(book, token, xsrfToken);

        return MockMvcRequestBuilders
                .post(url)
                .content("{\"title\":\"The Travelling Hornplayer\",\"foundOnGoogle\":true,\"googleBookId\":\"pbFgLK91crUC\",\"author\":\"xzczx\",\"genre\":\"zcxzx\",\"summary\":\"xzcxzczxc\",\"rating\":4,\"createdDateTime\":\"2018-08-12T17:28:25.435Z\"}")
                .headers(entity.getHeaders())
                .accept(MediaType.APPLICATION_JSON);
    }

}
