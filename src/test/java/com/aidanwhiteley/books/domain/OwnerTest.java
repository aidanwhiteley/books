package com.aidanwhiteley.books.domain;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.aidanwhiteley.books.domain.User.AuthenticationProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class OwnerTest {

    @Test
    void testBoilerPlateMethodsForCoverage() {
        Owner owner1 = new Owner("serviceId", "firstname", "lastName", "fullName", "example@example.com", "a link",
                "a picture", AuthenticationProvider.FACEBOOK);
        Owner owner2 = new Owner("serviceId", "firstname", "lastName", "fullName", "example@example.com", "a link",
                "a picture", AuthenticationProvider.FACEBOOK);

        assertEquals(owner1, owner2);
        assertEquals(owner1.hashCode(), owner2.hashCode());
        assertEquals(owner1.toString(), owner2.toString());
    }

    @Test
    void testForInvalidRole() {
        User aUser = new User();
        aUser.setRoles(Collections.singletonList(User.Role.ROLE_ACTUATOR));
        Comment aComment = new Comment();

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(Comment.class).setLevel(Level.valueOf("OFF"));

        Assertions.assertThrows(IllegalStateException.class, () -> aComment.setPermissionsAndContentForUser(aUser));

        context.getLogger(Comment.class).setLevel(Level.valueOf("OFF"));
    }

    @Test
    void testEditorCanDeleteOwnComment() {
        User aUser = new User();
        aUser.setRoles(Collections.singletonList(User.Role.ROLE_EDITOR));
        aUser.setAuthenticationServiceId("anAuthServId");
        aUser.setAuthProvider(AuthenticationProvider.GOOGLE);

        Owner owner = new Owner(aUser);
        Comment aComment = new Comment("Some comment", owner);
        aComment.setPermissionsAndContentForUser(aUser);
        assertTrue(aComment.isAllowDelete());

        aUser.setRoles(Collections.singletonList(User.Role.ROLE_USER));
        aComment.setPermissionsAndContentForUser(aUser);
        assertFalse(aComment.isAllowDelete());
    }

}
