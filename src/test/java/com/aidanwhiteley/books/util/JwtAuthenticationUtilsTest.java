package com.aidanwhiteley.books.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.aidanwhiteley.books.controller.jwt.JwtAuthentication;
import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
        assertEquals(User.Role.ROLE_ACTUATOR, userFromToken.getRoles().getFirst(), "Actuator user should only have actuator role");
        assertEquals(User.AuthenticationProvider.LOCAL, userFromToken.getAuthProvider(), "Actuator authprovider should be local");
    }

    @Test
    void tryToGetUserFromPrincipal() {

        // For dumb coverage reasons we want some debug logging to run - it does give us a chance to view the output
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(JwtAuthenticationUtils.class).setLevel(Level.valueOf("DEBUG"));

        Optional<User> aUser = jwtAuthenticationUtils.extractUserFromPrincipal(getPrincipal(), false);
        assertFalse(aUser.isPresent(), "Dummy user shouldnt be found in database");

        context.getLogger(JwtAuthenticationUtils.class).setLevel(Level.valueOf("WARN"));
    }

    @Test
    void throwExceptionForUnexpectedAuth() {
        final var auth = new JwtAuthentication("d\tummy", "d\rummy", "d\nummy");
        Assertions.assertThrows(IllegalStateException.class, () -> JwtAuthenticationUtils.handleUnexpectedAuth(auth));
    }

    private Principal getPrincipal() {
        return new JwtAuthentication("Dummy Name", User.AuthenticationProvider.LOCAL.toString(), "Some Auth Service ID");
    }
}
