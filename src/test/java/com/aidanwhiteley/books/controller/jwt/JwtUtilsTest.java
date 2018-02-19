package com.aidanwhiteley.books.controller.jwt;

import com.aidanwhiteley.books.controller.BookControllerTestUtils;
import com.aidanwhiteley.books.domain.User;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.Test;
import io.jsonwebtoken.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class JwtUtilsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtilsTest.class);
    @Test
    public void testCreadAndReadGoodToken() {
        JwtUtils jwt = new JwtUtils();

        jwt.setIssuer("A test issuer");
        jwt.setExpiryInMilliSeconds(60 * 1000);
        jwt.setSecretKey("A test secret key");

        User testUser = BookControllerTestUtils.getTestUser();
        testUser.addRole(User.Role.ROLE_ADMIN);
        String token = jwt.createTokenForUser(testUser);
        LOGGER.debug("Token was: {}", token);
        User userFromToken = jwt.getUserFromToken(token);

        assertEquals(testUser.getFullName(), userFromToken.getFullName());
    }

    @Test(expected = SignatureException.class)
    public void testTamperedWithToken() {
        JwtUtils jwt = new JwtUtils();

        jwt.setIssuer("A test issuer");
        jwt.setExpiryInMilliSeconds(60 * 1000);
        jwt.setSecretKey("A test secret key");

        User testUser = BookControllerTestUtils.getTestUser();
        String token = jwt.createTokenForUser(testUser);

        StringBuilder tampered = new StringBuilder(token);
        int strlength = token.length();
        char aChar = token.charAt(strlength - 1);
        tampered.setCharAt(strlength - 1, (char)( aChar - 1));

        jwt.getUserFromToken(tampered.toString());
    }

    @Test(expected = ExpiredJwtException.class)
    public void testExpiredToken() {
        JwtUtils jwt = new JwtUtils();

        jwt.setIssuer("A test issuer");
        jwt.setExpiryInMilliSeconds(-1);
        jwt.setSecretKey("A test secret key");

        User testUser = BookControllerTestUtils.getTestUser();
        String token = jwt.createTokenForUser(testUser);

        jwt.getUserFromToken(token);
    }
}
