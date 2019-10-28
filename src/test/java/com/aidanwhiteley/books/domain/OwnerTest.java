package com.aidanwhiteley.books.domain;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.aidanwhiteley.books.controller.config.WebSecurityConfiguration;
import org.junit.Test;

import com.aidanwhiteley.books.domain.User.AuthenticationProvider;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.security.AuthProvider;
import java.util.Arrays;

public class OwnerTest {

	@Test
	public void testBoilerPlateMethodsForCoverage() {
		Owner owner1 = new Owner("serviceId", "firstname", "lastName", "fullName", "example@example.com", "a link",
				"a picture", AuthenticationProvider.FACEBOOK);
		Owner owner2 = new Owner("serviceId", "firstname", "lastName", "fullName", "example@example.com", "a link",
				"a picture", AuthenticationProvider.FACEBOOK);
		
		assertEquals(owner1, owner2);
		assertEquals(owner1.hashCode(), owner2.hashCode());
		assertEquals(owner1.toString(), owner2.toString());
	}

	@Test(expected = IllegalStateException.class)
	public void testForInvalidRole() {
		User aUser = new User();
		aUser.setRoles(Arrays.asList(User.Role.ROLE_ACTUATOR));
		Comment aComment = new Comment();

		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Comment.class).setLevel(Level.valueOf("OFF"));

		aComment.setPermissionsAndContentForUser(aUser);

		context.getLogger(Comment.class).setLevel(Level.valueOf("OFF"));
	}

	@Test
	public void testEditorCanDeleteOwnComment() {
		User aUser = new User();
		aUser.setRoles(Arrays.asList(User.Role.ROLE_EDITOR));
		aUser.setAuthenticationServiceId("anAuthServId");
		aUser.setAuthProvider(AuthenticationProvider.GOOGLE);

		Owner owner = new Owner(aUser);
		Comment aComment = new Comment("Some comment", owner);
		aComment.setPermissionsAndContentForUser(aUser);
		assertTrue(aComment.isAllowDelete());

		aUser.setRoles(Arrays.asList(User.Role.ROLE_USER));
		aComment.setPermissionsAndContentForUser(aUser);
		assertFalse(aComment.isAllowDelete());
	}

}
