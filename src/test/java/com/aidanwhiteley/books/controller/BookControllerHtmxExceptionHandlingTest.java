package com.aidanwhiteley.books.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookControllerHtmxExceptionHandlingTest {

    private final BookControllerHtmxExceptionHandling exceptionHandling = new BookControllerHtmxExceptionHandling() {
        @Override
        public void addUserToModel(Principal principal, Model model) {
            // Nothing to add to model for these tests.
        }
    };

    @Test
    void testHandleGoogleBooksRateLimitException() {
        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("hx-request", "true");
        WebRequest webRequest = new ServletWebRequest(request);

        HttpClientErrorException.TooManyRequests ex = getTooManyRequestsException();
        String view = exceptionHandling.handleGoogleBooksRateLimitException(ex, model, null, webRequest);

        assertEquals("error :: cloudy-error-detail", view);
        assertEquals("e-google-books-429", model.getAttribute("code"));
        assertTrue(((String) model.getAttribute("description")).contains("API key"));
    }

    @Test
    void testHandleGoogleBooksRateLimitExceptionNonHtmxRequest() {
        ExtendedModelMap model = new ExtendedModelMap();
        WebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());

        HttpClientErrorException.TooManyRequests ex = getTooManyRequestsException();
        String view = exceptionHandling.handleGoogleBooksRateLimitException(ex, model, null, webRequest);

        assertEquals("error", view);
        assertEquals("e-google-books-429", model.getAttribute("code"));
        assertTrue(((String) model.getAttribute("description")).contains("rate limiting requests"));
    }

    private HttpClientErrorException.TooManyRequests getTooManyRequestsException() {
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        return (HttpClientErrorException.TooManyRequests) exception;
    }
}
