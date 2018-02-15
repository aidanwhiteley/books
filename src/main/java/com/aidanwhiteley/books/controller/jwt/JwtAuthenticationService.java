package com.aidanwhiteley.books.controller.jwt;

import com.aidanwhiteley.books.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
public class JwtAuthenticationService {

    public static final String JWT_COKKIE_NAME = "jwt";

    @Autowired
    JwtUtils jwtUtils;

    @Value("${books.jwt.cookieOverHttpsOnly}")
    private boolean cookieOverHttpsOnly;

    @Value("${books.jwt.cookieAccessedByHttoOnly}")
    private boolean cookieAccessedByHttoOnly;

    @Value("${books.client.xsrfHeader}")
    private String xsrfHeader;

    @Value("${books.client.allowedCorsOrigin}")
    private String allowedCorsOrigin;

    @Value("${books.client.enableCORS}")
    private boolean enableCORS;

    public void setAuthenticationData(HttpServletRequest request, HttpServletResponse response, Authentication authentication, User user) {
        String token = jwtUtils.createTokenForUser(user);

        Cookie cookie = new Cookie(JWT_COKKIE_NAME, token);

        if (cookieAccessedByHttoOnly) {
            cookie.setHttpOnly(true);
        }
        if (cookieOverHttpsOnly) {
            cookie.setSecure(true);
        }
        response.addCookie(cookie);

        // There's no point in setting
        if (!enableCORS) {
            response.setHeader(xsrfHeader, token);
            response.setStatus(HttpStatus.NO_CONTENT.value());
        }
    }

    public void readAndValidateAuthenticationData() {

    }
}
