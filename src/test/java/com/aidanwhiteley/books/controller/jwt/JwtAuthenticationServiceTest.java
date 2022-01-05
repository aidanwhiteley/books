package com.aidanwhiteley.books.controller.jwt;

import com.aidanwhiteley.books.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtAuthenticationServiceTest {

    @Test
    void testSetAuthenticationData() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        User aUser = new User();
        final String aFullName = "Marilyn Monroe";
        aUser.setFullName(aFullName);

        JwtUtils jwtUtils = new JwtUtils();
        jwtUtils.setSecretKey("Blah");

        JwtAuthenticationService theService = new JwtAuthenticationService(jwtUtils);
        theService.setCookieExpirySeconds(Integer.MAX_VALUE);
        theService.setCookieOverHttpsOnly(true);
        theService.setCookieAccessedByHttpOnly(true);
        theService.setCookieSameSiteStrict(true);
        theService.setCookieAccessedByHttpOnly(true);
        theService.setJwtCookiePath("/dummyPath");

        theService.setAuthenticationData(response, aUser);

        String cookie = response.getHeaders("Set-Cookie").stream().
                filter(s -> s.contains(JwtAuthenticationService.JWT_COOKIE_NAME)).findFirst()
                .orElse(null);
        assertNotNull(cookie);

        // In v0.14.1 support for SameSite cookie attribute was added. Test that it exists.
        String cookieForSameSite = response.getHeaders("Set-Cookie").stream().
                filter(s -> s.contains("SameSite=Strict")).findFirst()
                .orElse(null);
        assertNotNull(cookieForSameSite);

        String cookieHttpOnly = response.getHeaders("Set-Cookie").stream().
                filter(s -> s.contains("HttpOnly")).findFirst()
                .orElse(null);
        assertNotNull(cookieHttpOnly);

        String cookieAge = response.getHeaders("Set-Cookie").stream().
                filter(s -> s.contains("Max-Age=" + Integer.MAX_VALUE)).findFirst()
                .orElse(null);
        assertNotNull(cookieAge);

        String cookiePath = response.getHeaders("Set-Cookie").stream().
                filter(s -> s.contains("Path=/dummyPath")).findFirst()
                .orElse(null);
        assertNotNull(cookiePath);

    }
}
