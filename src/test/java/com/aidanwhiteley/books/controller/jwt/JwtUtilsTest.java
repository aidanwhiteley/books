package com.aidanwhiteley.books.controller.jwt;

import com.aidanwhiteley.books.controller.BookControllerTestUtils;
import com.aidanwhiteley.books.domain.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SecurityException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtilsTest.class);

    @Test
    void testCreateAndReadGoodToken() {
        JwtUtils jwt = new JwtUtils();

        jwt.setIssuer("A test issuer");
        jwt.setExpiryInMilliSeconds(60 * 1000);
        jwt.setSecretKey(JwtUtils.createRandomBase64EncodedSecretKey());

        User testUser = BookControllerTestUtils.getTestUser();
        testUser.addRole(User.Role.ROLE_ADMIN);
        String token = jwt.createTokenForUser(testUser);
        LOGGER.debug("Token was: {}", token);
        User userFromToken = jwt.getUserFromToken(token);

        assertEquals(testUser.getFullName(), userFromToken.getFullName());
    }

    @Test
    void testTamperedWithToken() {
        JwtUtils jwt = new JwtUtils();

        jwt.setIssuer("A test issuer");
        jwt.setExpiryInMilliSeconds(60 * 1000);
        jwt.setSecretKey(JwtUtils.createRandomBase64EncodedSecretKey());

        User testUser = BookControllerTestUtils.getTestUser();
        String token = jwt.createTokenForUser(testUser);

        StringBuilder tampered = new StringBuilder(token);
        int strlength = token.length();
        char aChar = token.charAt(strlength - 1);
        tampered.setCharAt(strlength - 1, (char) (aChar - 1));
        String tamperedString = tampered.toString();

        try {
            jwt.getUserFromToken(tamperedString);
            fail("Expected a SecurityException to be thrown " +
                    "- actually the deprecated SignatureException sub class if before V1.0 jsonwebtoken");
        } catch (JwtException je) {
            assertInstanceOf(SecurityException.class, je);
        }

    }

    @Test
    void testExpiredToken() {
        JwtUtils jwt = new JwtUtils();

        jwt.setIssuer("A test issuer");
        jwt.setExpiryInMilliSeconds(-1);
        jwt.setSecretKey(JwtUtils.createRandomBase64EncodedSecretKey());

        User testUser = BookControllerTestUtils.getTestUser();
        String token = jwt.createTokenForUser(testUser);

        Assertions.assertThrows(ExpiredJwtException.class, () -> jwt.getUserFromToken(token));
    }
}
