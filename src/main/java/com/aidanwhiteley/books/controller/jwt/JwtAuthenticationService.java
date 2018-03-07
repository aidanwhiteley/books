package com.aidanwhiteley.books.controller.jwt;

import com.aidanwhiteley.books.domain.User;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
public class JwtAuthenticationService {

    public static final String JWT_COOKIE_NAME = "JWT";
    public static final String JSESSIONID_COOKIE_NAME = "JSESSIONID";
    public static final String XSRF_HEADER_NAME = "X-XSRF-TOKEN";
    public static final String XSRF_COOKIE_NAME = "XSRF-TOKEN";

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationService.class);

    private final JwtUtils jwtUtils;

    @Value("${books.jwt.cookieOverHttpsOnly}")
    private boolean cookieOverHttpsOnly;

    @Value("${books.jwt.cookieAccessedByHttpOnly}")
    private boolean cookieAccessedByHttpOnly;

    @Value("${books.jwt.cookiePath}")
    private String jwtCookiePath;

    @Value("${books.jwt.cookieExpirySeconds}")
    private int cookieExpirySeconds;

    @Value("${books.client.xsrfHeader}")
    private String xsrfHeader;

    @Value("${books.client.allowedCorsOrigin}")
    private String allowedCorsOrigin;

    @Value("${books.client.enableCORS}")
    private boolean enableCORS;

    @Autowired
    public JwtAuthenticationService(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    public void setAuthenticationData(HttpServletResponse response, User user) {

        String token = jwtUtils.createTokenForUser(user);

        Cookie cookie = new Cookie(JWT_COOKIE_NAME, token);

        cookie.setHttpOnly(cookieAccessedByHttpOnly);
        cookie.setSecure(cookieOverHttpsOnly);
        cookie.setPath(jwtCookiePath);
        cookie.setMaxAge(cookieExpirySeconds);

        response.addCookie(cookie);
        LOGGER.debug("JWT cookie written for {}", user.getFullName());

    }

    public JwtAuthentication readAndValidateAuthenticationData(HttpServletRequest request, HttpServletResponse response) {

        LOGGER.debug("Running JwtAuthenticationService - readAndValidateAuthenticationData");

        JwtAuthentication auth = null;

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                LOGGER.debug("Found cookie named: {}", cookie.getName());
                if (cookie.getName().equals(JWT_COOKIE_NAME)) {
                    String token = cookie.getValue();
                    if (token == null || token.trim().isEmpty()) {
                        LOGGER.warn("JWT cookie found but was empty - we will look to remove this later");
                    } else {
                        try {
                            User user = jwtUtils.getUserFromToken(token);
                            auth = new JwtAuthentication(user);
                            // If we got to here with no exceptions thrown
                            // then we can assume we have a valid token
                            auth.setAuthenticated(true);
                            LOGGER.debug("JWT found and validated - setting authentication true");
                        } catch (ExpiredJwtException eje) {
                            expireJwtCookie(response);
                            LOGGER.info("JWT expired so cookie deleted");
                        } catch (RuntimeException re) {
                            expireJwtCookie(response);
                            LOGGER.warn("Error validating jwt token: {}. So cookie deleted", re.getMessage(), re);
                        }
                    }
                } else if (cookie.getName().equals(JSESSIONID_COOKIE_NAME)) {
                    // The Spring Oauth2 code is using HTTP session to store data during the
                    // "redirect dance" with the autentication providers.
                    // Otherwise this application doesnt use HTTP session state.
                    LOGGER.debug("Found a JSESSIONID based cookie");
                }
            }
        }

        return auth;
    }

    public void expireJwtCookie(HttpServletResponse response) {
        Cookie emptyCookie = new Cookie(JWT_COOKIE_NAME, "");
        emptyCookie.setMaxAge(0);
        emptyCookie.setHttpOnly(cookieAccessedByHttpOnly);
        emptyCookie.setSecure(cookieOverHttpsOnly);
        emptyCookie.setPath(jwtCookiePath);
        response.addCookie(emptyCookie);
    }

    public void expireXsrfCookie(HttpServletResponse response) {
        Cookie emptyCookie = new Cookie(XSRF_COOKIE_NAME, "");
        emptyCookie.setMaxAge(0);
        emptyCookie.setHttpOnly(false);
        emptyCookie.setSecure(cookieOverHttpsOnly);
        emptyCookie.setPath(jwtCookiePath);
        response.addCookie(emptyCookie);
    }

    public void expireJsessionIdCookie(HttpServletResponse response) {
        Cookie emptyCookie = new Cookie(JSESSIONID_COOKIE_NAME, "");
        emptyCookie.setMaxAge(0);
        emptyCookie.setHttpOnly(true);
        emptyCookie.setSecure(cookieOverHttpsOnly);
        emptyCookie.setPath(jwtCookiePath);
        response.addCookie(emptyCookie);
    }

}
