package com.aidanwhiteley.books.controller.jwt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JwtAuthenticationTest {
	
	@Test
	public void testJwtAuthenticationDetails() {
		JwtAuthentication auth1 = new JwtAuthentication("Full Name", "AuthProvider", "AuthServiceId");
		JwtAuthentication auth2 = new JwtAuthentication("Full Name", "AuthProvider", "AuthServiceId");
		assertEquals(auth1, auth2);
		assertEquals(auth1.toString(), auth2.toString());
		assertEquals(auth1.hashCode(), auth2.hashCode());
	}

}
