package com.aidanwhiteley.books.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.aidanwhiteley.books.domain.User.AuthenticationProvider;

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

}
