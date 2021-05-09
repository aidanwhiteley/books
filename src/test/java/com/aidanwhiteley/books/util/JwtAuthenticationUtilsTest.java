package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.controller.jwt.JwtAuthentication;
import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtAuthenticationUtilsTest extends IntegrationTest {

    @Autowired
    private JwtAuthenticationUtils jwtAuthenticationUtils;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    void tryToGetActuatorJwtToken() {

        String jwt = jwtAuthenticationUtils.getJwtForActuatorRoleUser();
        User userFromToken = jwtUtils.getUserFromToken(jwt);

        assertEquals(1, userFromToken.getRoles().size(), "Actuator user should have single role");
        assertEquals(User.Role.ROLE_ACTUATOR, userFromToken.getRoles().get(0), "Actuator user should only have actuator role");
        assertEquals(User.AuthenticationProvider.LOCAL, userFromToken.getAuthProvider(), "Actuator authprovider should be local");
    }

    @Test
    void throwExceptionForUnexpectedAuth() {
        final var auth = new JwtAuthentication("d\tummy", "d\rummy", "d\nummy");
        Assertions.assertThrows(IllegalStateException.class, () -> JwtAuthenticationUtils.handleUnexpectedAuth(auth));
    }
}
