package com.aidanwhiteley.books.controller.jwt;

import com.aidanwhiteley.books.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertNotNull;

public class JwtAuthenticationServiceTest {

    @Test
    public void testSetAuthenticationData() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        User aUser = new User();
        final String aFullName = "Marilyn Monroe";
        aUser.setFullName(aFullName);

        JwtUtils jwtUtils = new JwtUtils();
        jwtUtils.setSecretKey("Blah");
        JwtAuthenticationService theService = new JwtAuthenticationService(jwtUtils);
        theService.setAuthenticationData(response, aUser);

        String cookie = response.getHeaders("Set-Cookie").stream().
                filter(s -> s.contains(JwtAuthenticationService.JWT_COOKIE_NAME)).findFirst()
                .orElse(null);

        assertNotNull(cookie);
    }
}
