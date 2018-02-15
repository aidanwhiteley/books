package com.aidanwhiteley.books.controller.jwt;

import com.aidanwhiteley.books.domain.User;
import com.sun.deploy.net.HttpResponse;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
public class JwtAuthenticationService {

    public static final String JWT_COOKIE_NAME = "jwt";

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationService.class);

    @Autowired
    JwtUtils jwtUtils;

    @Value("${books.jwt.cookieOverHttpsOnly}")
    private boolean cookieOverHttpsOnly;

    @Value("${books.jwt.cookieAccessedByHttoOnly}")
    private boolean cookieAccessedByHttoOnly;

    @Value("${books.jwt.cookiePath}")
    private String cookiePath;

    @Value("${books.client.xsrfHeader}")
    private String xsrfHeader;

    @Value("${books.client.allowedCorsOrigin}")
    private String allowedCorsOrigin;

    @Value("${books.client.enableCORS}")
    private boolean enableCORS;

    public void setAuthenticationData(HttpServletRequest request, HttpServletResponse response, User user) {

        String token = jwtUtils.createTokenForUser(user);

        Cookie cookie = new Cookie(JWT_COOKIE_NAME, token);

        if (cookieAccessedByHttoOnly) {
            cookie.setHttpOnly(true);
        }
        if (cookieOverHttpsOnly) {
            cookie.setSecure(true);
        }
        cookie.setPath(cookiePath);

        response.addCookie(cookie);
        LOGGER.debug("JWT cookie written for {}", user.getFullName());

        // We dont bother setting a CSRF header here as after authentication
        // we typically expect that there will be a 30x redirect sent to the
        // client and if that is the case, there is no way for the client side
        // code to read and store a response header before the browser automatically
        // follows the re-direct.
        // So we set a CSRF header elsewhere in teh code base - not here.
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
