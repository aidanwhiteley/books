package com.aidanwhiteley.books.controller.jwt;

import com.aidanwhiteley.books.controller.BookControllerTestUtils;
import com.aidanwhiteley.books.domain.User;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class JwtAuthenticationServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationServiceTest.class);

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
