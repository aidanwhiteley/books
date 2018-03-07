package com.aidanwhiteley.books.domain;

import static org.junit.Assert.*;

import java.time.LocalDateTime;

import org.junit.Test;

import com.aidanwhiteley.books.domain.User.AuthenticationProvider;

public class UserTest {

	@Test
	public void testBoilerPlateGeneratedMethodsJustForCodeCoverage() {
		LocalDateTime now = LocalDateTime.now();
		User user1 = buildUser(now);
		User user2 = buildUser(now);
		
		assertEquals(user1.toString(), user2.toString());
		assertEquals(user1.hashCode(), user2.hashCode());
		assertTrue(user1.equals(user2));
	}

	private User buildUser(LocalDateTime now) {
		return User.builder(). 
				authenticationServiceId("Google"). 
				authProvider(AuthenticationProvider.GOOGLE). 
				email("example@example.com"). 
				firstLogon(now). 
				firstName("First"). 
				fullName("First Last"). 
				lastLogon(now). 
				lastName("Last"). 
				link("A link"). 
				picture("A picture"). 
				build();
	}

}
