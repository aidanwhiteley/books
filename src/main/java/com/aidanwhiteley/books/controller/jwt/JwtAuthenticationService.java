package com.aidanwhiteley.books.controller.jwt;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aidanwhiteley.books.domain.User;

import io.jsonwebtoken.ExpiredJwtException;

@Service
public class JwtAuthenticationService {

    public static final String JWT_COOKIE_NAME = "jwt";

    public static final String JSESSIONID_COOKIE_NAME = "JSESSIONID";

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationService.class);

    @Autowired
    JwtUtils jwtUtils;

    @Value("${books.jwt.cookieOverHttpsOnly}")
    private boolean cookieOverHttpsOnly;

    @Value("${books.jwt.cookieAccessedByHttpOnly}")
    private boolean cookieAccessedByHttpOnly;

    @Value("${books.jwt.cookiePath}")
    private String cookiePath;

    @Value("${books.jwt.cookieExpirySeconds}")
    private int cookieExpirySeconds;

    @Value("${books.client.xsrfHeader}")
    private String xsrfHeader;

    @Value("${books.client.allowedCorsOrigin}")
    private String allowedCorsOrigin;

    @Value("${books.client.enableCORS}")
    private boolean enableCORS;

    public void setAuthenticationData(HttpServletRequest request, HttpServletResponse response, User user) {

        String token = jwtUtils.createTokenForUser(user);

        Cookie cookie = new Cookie(JWT_COOKIE_NAME, token);

        if (cookieAccessedByHttpOnly) {
            cookie.setHttpOnly(true);
        }
        if (cookieOverHttpsOnly) {
            cookie.setSecure(true);
        }
        cookie.setPath(cookiePath);
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
                        LOGGER.warn("JWT cookie found but was empty");
                    } else {
                        try {
                            User user = jwtUtils.getUserFromToken(token);
                            auth = new JwtAuthentication(user);
                            // If we got to here with no exceptions thrown
                            // then we can assume we have a valid token
                            auth.setAuthenticated(true);
                            LOGGER.debug("JWT found and validated - setting authentication true");
                        } catch (ExpiredJwtException eje) {
                            expireCookie(response, cookie);
                            LOGGER.info("JWT expired so cookie deleted");
                        } catch (RuntimeException re) {
                            expireCookie(response, cookie);
                            LOGGER.warn("Error validating jwt token: {}. So cookie deleted", re.getMessage(), re);
                        }
                    }
                } else if (cookie.getName().equals(JSESSIONID_COOKIE_NAME)) {
                    // I've no idea what framework code is creating a JSESSIONID cookie
                    // (and presumably an actual HTTP session.
                    // Its not needed.
                    // To stop any chance of it being used / relied upon, the JSESSIONID
                    // based cookie is removed.
                	LOGGER.debug("Found an unwated JSESSIONID based cookie - removing it");
                	expireCookie(response, cookie);
                	// We don't remove the session - we'd have race conditions with
                    // other API calls that might also try remove the session - leading
                    // to possible NPEs.
                }
            }
        }

        return auth;
    }

    private void expireCookie(HttpServletResponse response, Cookie cookie) {
        // Anything wrong with token - delete it from cookie
        Cookie emptyCookie = new Cookie(JWT_COOKIE_NAME, "");
        cookie.setMaxAge(0);
        response.addCookie(emptyCookie);
    }
}
