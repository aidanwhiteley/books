package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.controller.dtos.ClientRoles;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.GOOGLE;
import static com.aidanwhiteley.books.domain.User.Role.ROLE_ADMIN;
import static com.aidanwhiteley.books.domain.User.Role.ROLE_EDITOR;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserRepositoryTest extends IntegrationTest {

    private static final String FULL_NAME = "A test user";

    private static final String SERVICE_ID = "abcd";

    @Autowired
    private UserRepository userRepository;

    private static User createTestUser() {
        return User.builder().authenticationServiceId(SERVICE_ID).authProvider(GOOGLE).
                fullName(FULL_NAME).adminEmailedAboutSignup(true).build();
    }

    @Test
    void createAndFindUser() {
        User user = createTestUser();

        userRepository.insert(user);

        List<User> users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(SERVICE_ID, GOOGLE.toString());
        assertEquals(1, users.size());
        assertEquals(FULL_NAME, users.getFirst().getFullName());
    }

    @Test
    void updateUserRoles() {

        // Tidy up before test
        List<User> oldUsers = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(SERVICE_ID, GOOGLE.toString());
        for (User user : oldUsers) {
            userRepository.deleteById(user.getId());
        }

        User user = createTestUser();
        userRepository.insert(user);
        List<User> users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(SERVICE_ID, GOOGLE.toString());
        assertEquals(0, users.getFirst().getRoles().size());

        ClientRoles clientRoles = new ClientRoles(users.getFirst().getId(), false, true);
        long updatesCount = userRepository.updateUserRoles(clientRoles);
        assertEquals(1, updatesCount);
        users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(SERVICE_ID, GOOGLE.toString());
        assertEquals(1, users.getFirst().getRoles().size());
        assertEquals(ROLE_EDITOR, users.getFirst().getRoles().getFirst());

        clientRoles = new ClientRoles(users.getFirst().getId(), true, true);
        userRepository.updateUserRoles(clientRoles);
        users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(SERVICE_ID, GOOGLE.toString());
        assertEquals(2, users.getFirst().getRoles().size());
        assertEquals(ROLE_ADMIN, users.getFirst().getRoles().getFirst());
        assertEquals(ROLE_EDITOR, users.getFirst().getRoles().get(1));

        clientRoles = new ClientRoles(users.getFirst().getId(), false, false);
        userRepository.updateUserRoles(clientRoles);
        users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(SERVICE_ID, GOOGLE.toString());
        assertEquals(0, users.getFirst().getRoles().size());
    }

}
