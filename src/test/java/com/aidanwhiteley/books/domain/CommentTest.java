package com.aidanwhiteley.books.domain;

import static org.junit.Assert.*;

import java.time.LocalDateTime;

import org.junit.Test;

import com.aidanwhiteley.books.domain.User.AuthenticationProvider;

public class CommentTest {

	@Test
	public void testBoilerPlates() {
		Owner anOwner = new Owner("authid1", "firstname", "lastName", "fullName", 
				"example@example.com", "a link", "a picture", AuthenticationProvider.FACEBOOK);
		LocalDateTime now = LocalDateTime.now();
				
		Comment comment1 = new Comment("testCommentText1", anOwner, now);
		comment1.setId("dummyUsuallyGenerated");
		Comment comment2 = new Comment("testCommentText1", anOwner, now);
		comment2.setId("dummyUsuallyGenerated");
		
		assertEquals(comment1, comment2);
		assertEquals(comment1.hashCode(), comment2.hashCode());
		assertEquals(comment1.toString(), comment2.toString());
	}

}
