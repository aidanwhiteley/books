package com.aidanwhiteley.books.controller.config;

import com.aidanwhiteley.books.controller.exceptions.JwtAuthAuzException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

/**
 * Based on https://stackoverflow.com/questions/49095383/spring-security-5-stateless-oauth2-login-how-to-implement-cookies-based-author
 */
class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpCookieOAuth2AuthorizationRequestRepository.class);

    @Value("${books.oauth2.cookieOverHttpsOnly}")
    private boolean cookieOverHttpsOnly;

    private final ObjectMapper authRequestJsonMapper;

    public static final String COOKIE_NAME = "cloudy-oauth2-auth";

    public HttpCookieOAuth2AuthorizationRequestRepository(ObjectMapper authRequestJsonMapper) {
        this.authRequestJsonMapper = authRequestJsonMapper;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return fetchCookie(request)
                .map(this::toOAuth2AuthorizationRequest)
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
                                         HttpServletResponse response) {

        if (authorizationRequest == null) {
            deleteCookie(request, response);
            return;
        }

        Cookie cookie = new Cookie(COOKIE_NAME, fromAuthorizationRequest(authorizationRequest));
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieOverHttpsOnly);
        cookie.setMaxAge(-1);   // Expire when browser closed - bug in API means explicit removal not possible
        response.addCookie(cookie);     // lgtm[java/insecure-cookie]
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(request, response);
        return loadAuthorizationRequest(request);
    }
    
    private String fromAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            return Base64.getEncoder().encodeToString(authRequestJsonMapper.writeValueAsString(authorizationRequest).getBytes());
        } catch (JsonProcessingException jspe) {
            var msg = "Failed to serialise OAuth auth to JSON";
            LOGGER.error(msg, jspe);
            throw new JwtAuthAuzException(msg);
        }
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response) {
        fetchCookie(request).ifPresent(cookie -> {
            // Delete the cookie from the response while leaving it in the request
            Cookie responseCookie = new Cookie(COOKIE_NAME, "");
            responseCookie.setPath("/");
            responseCookie.setMaxAge(0);
            responseCookie.setSecure(cookieOverHttpsOnly);
            responseCookie.setHttpOnly(true);
            response.addCookie(responseCookie);     // lgtm[java/insecure-cookie]
        });
    }

    private Optional<Cookie> fetchCookie(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(COOKIE_NAME)) {
                    return Optional.of(cookie);
                }
            }
        }

        return Optional.empty();
    }

    private OAuth2AuthorizationRequest toOAuth2AuthorizationRequest(Cookie cookie) {
        try {
            return authRequestJsonMapper.readValue(new String(Base64.getDecoder().decode(cookie.getValue())),
                    OAuth2AuthorizationRequest.class);
        } catch (IOException jspe) {
            var msg = "Failed to deserialise OAuth auth from JSON";
            LOGGER.error(msg, jspe);
            throw new JwtAuthAuzException(msg);
        }
    }
}