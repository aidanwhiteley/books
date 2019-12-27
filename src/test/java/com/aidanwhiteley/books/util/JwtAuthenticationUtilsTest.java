package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.controller.jwt.JwtAuthentication;
import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.User;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

public class JwtAuthenticationUtilsTest extends IntegrationTest {

    @Autowired
    private JwtAuthenticationUtils jwtAuthenticationUtils;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    public void tryToGetActuatorJwtToken() {

        String jwt = jwtAuthenticationUtils.getJwtForActuatorRoleUser();
        User userFromToken = jwtUtils.getUserFromToken(jwt);

        assertEquals("Actuator user should have single role", 1, userFromToken.getRoles().size());
        assertEquals("Actuator user should only have actuator role", User.Role.ROLE_ACTUATOR, userFromToken.getRoles().get(0));
        assertEquals("Actuator authprovider should be local", User.AuthenticationProvider.LOCAL, userFromToken.getAuthProvider());
    }

    @Test(expected = IllegalStateException.class)
    public void throwExceptionForUnexpectedAuth() {
        JwtAuthenticationUtils.handleUnexpectedAuth(new JwtAuthentication("d\tummy", "d\rummy", "d\nummy"));
    }
}
