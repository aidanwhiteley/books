package com.aidanwhiteley.books.repository;

import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.GOOGLE;
import static com.aidanwhiteley.books.domain.User.Role.ROLE_ADMIN;
import static com.aidanwhiteley.books.domain.User.Role.ROLE_EDITOR;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.aidanwhiteley.books.controller.dtos.ClientRoles;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.util.IntegrationTest;

public class UserRepositoryTest extends IntegrationTest {
	
	private static final String FULL_NAME = "A test user";

	private static final String SERVICE_ID = "abcd";
	
	@Autowired
    UserRepository userRepository;
	
    @After
    public void tearDown() {
    	userRepository.deleteAll();
    }
	
	@Test
	public void createAndFindUser() {
		User user = createTestUser();
		
		userRepository.insert(user);
		
		List<User> users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(SERVICE_ID, GOOGLE.toString());
		assertEquals(users.size(), 1);
		assertEquals(users.get(0).getFullName(), FULL_NAME);
	}
	
	@Test
	public void updateUserRoles() {
		User user = createTestUser();
		userRepository.insert(user);
		List<User> users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(SERVICE_ID, GOOGLE.toString());
		assertEquals(users.get(0).getRoles().size(), 0);
		
		ClientRoles clientRoles = new ClientRoles(users.get(0).getId(), false, true);
		int updatesCount = userRepository.updateUserRoles(clientRoles);
		assertEquals(updatesCount, 1);
		users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(SERVICE_ID, GOOGLE.toString());
		assertEquals(users.get(0).getRoles().size(), 1);
		assertEquals(users.get(0).getRoles().get(0), ROLE_EDITOR);
		
		clientRoles = new ClientRoles(users.get(0).getId(), true, true);
		userRepository.updateUserRoles(clientRoles);
		users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(SERVICE_ID, GOOGLE.toString());
		assertEquals(users.get(0).getRoles().size(), 2);
		assertEquals(users.get(0).getRoles().get(0), ROLE_ADMIN);
		assertEquals(users.get(0).getRoles().get(1), ROLE_EDITOR);
		
		clientRoles = new ClientRoles(users.get(0).getId(), false, false);
		userRepository.updateUserRoles(clientRoles);
		users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(SERVICE_ID, GOOGLE.toString());
		assertEquals(users.get(0).getRoles().size(), 0);
	}

	public static User createTestUser() {
		return User.builder().authenticationServiceId(SERVICE_ID).authProvider(GOOGLE).fullName(FULL_NAME).build();
	}

}
