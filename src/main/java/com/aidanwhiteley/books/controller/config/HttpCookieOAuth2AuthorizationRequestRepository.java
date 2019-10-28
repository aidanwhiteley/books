package com.aidanwhiteley.books.controller.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.SerializationUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.Optional;

/**
 * Based on https://stackoverflow.com/questions/49095383/spring-security-5-stateless-oauth2-login-how-to-implement-cookies-based-author
 */
class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    @Value("${books.oauth2.cookieOverHttpsOnly}")
    private boolean cookieOverHttpsOnly;

    public static final String COOKIE_NAME = "cloudy-oauth2-auth";

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
        cookie.setSecure(cookieOverHttpsOnly);          // lgtm[java/insecure-cookie]
        cookie.setMaxAge(-1);   // Expire when browser closed - bug in API means explicit removal not possible
        response.addCookie(cookie);
    }

    /**
     * Removes the outh auth cookie.
     *
     * @param request  Thhtp Request
     * @return an OAuth2AuthorizationRequest
     * @deprecated Since Spring Boot 2.1.0 and associated Spring Security version
     */
    @Override
    @Deprecated
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request) {
        // Question: How to remove the cookie, because we don't have access to response object here.
        // This seems to be a flaw in the design of the AuthorizationRequestRepository interface
        // as the default behaviour is to remove data from the HTTP session -
        // which will be accessed via the request object. Here we
        // want to clear out a cookie for which we need access to the response object.
        // So, for the time being, another unrelated part of the code base clears the cookie -
        // see the JwtAuthenticationService class for details.
        // There was an issue raised on Spring Security for this and the interface may be
        // uplifted in 5.1 - see https://github.com/spring-projects/spring-security/issues/5313

        // Since Spring Boot 2.1 (Spring Security 5.1) this method is now deprecated and the
        // version below which provides access to the HttpServletResponse is preferred (as this
        // allows access to clearing out the cookie).
        return loadAuthorizationRequest(request);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(request, response);
        return loadAuthorizationRequest(request);
    }
    
    private String fromAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest) {
        return Base64.getUrlEncoder().encodeToString(
                SerializationUtils.serialize(authorizationRequest));
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response) {
        fetchCookie(request).ifPresent(cookie -> {
            // Delete the cookie from the response while leaving it in the request
            Cookie responseCookie = new Cookie(COOKIE_NAME, "");
            responseCookie.setPath("/");
            responseCookie.setMaxAge(0);
            responseCookie.setSecure(cookieOverHttpsOnly);          // lgtm[java/insecure-cookie]
            responseCookie.setHttpOnly(true);
            response.addCookie(responseCookie);
        });
    }

    private Optional<Cookie> fetchCookie(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();

        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(COOKIE_NAME)) {
                    return Optional.of(cookie);
                }
            }
        }

        return Optional.empty();
    }

    private OAuth2AuthorizationRequest toOAuth2AuthorizationRequest(Cookie cookie) {

        return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(
                Base64.getUrlDecoder().decode(cookie.getValue()));
    }
}