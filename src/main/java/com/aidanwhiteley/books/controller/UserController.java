package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.util.AuthenticationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.FACEBOOK;
import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.GOOGLE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/secure/api")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationUtils authUtils;


    @RequestMapping("/user")
    @PreAuthorize("hasAnyRole('ROLE_EDITOR', 'ROLE_ADMIN', 'ROLE_USER')")
    public User user(Principal principal) {

        Map<String, String> userDetails = authUtils.getRemoteUserDetails(principal);

        LOGGER.debug("Remote user details: " + userDetails);

        User user = authUtils.extractUserFromPrincipal(principal);
        User.AuthenticationProvider provider = authUtils.getAuthProviderFromPrincipal(principal);

        if (user == null) {
            return createUser(userDetails, provider);
        } else {
            return updateUser(userDetails, user, provider);
        }
    }

    @RequestMapping(value = "/users", method = GET)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<User> users(Principal principal) {
        return userRepository.findAll();
    }

    @RequestMapping(value = "/users/{id}", method = DELETE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void deleteBookById(@PathVariable("id") String id, Principal principal) {

        User user = authUtils.extractUserFromPrincipal(principal);
        if (user.getId().equals(id)) {
            LOGGER.warn("User {} on {} attempted to delete themselves. This isn't allowed", user.getFullName(), user.getAuthProvider());
            throw new IllegalStateException("You cannot delete yourself! Logon with a different admin user if you really want delete this user");
        }

        userRepository.delete(id);
    }

    private User createUser(Map<String, String> userDetails, User.AuthenticationProvider provider) {

        User user = null;

        switch (provider) {
            case GOOGLE: {
                user = User.builder().authenticationServiceId(userDetails.get("id")).
                        firstName(userDetails.get("given_name")).
                        lastName(userDetails.get("family_name")).
                        fullName(userDetails.get("name")).
                        link(userDetails.get("link")).
                        picture(userDetails.get("picture")).
                        email(userDetails.get("email")).
                        lastLogon(LocalDateTime.now()).
                        firstLogon(LocalDateTime.now()).
                        authProvider(GOOGLE).
                        role(User.Role.ROLE_USER).
                        build();
                break;
            }
            case FACEBOOK: {
                user = User.builder().authenticationServiceId(userDetails.get("id")).
                        firstName(userDetails.get("first_name")).
                        lastName(userDetails.get("last_name")).
                        fullName(userDetails.get("name")).
                        link(userDetails.get("link")).
                        // picture(userDetails.get("picture.data.url")).
                                email(userDetails.get("email")).
                                lastLogon(LocalDateTime.now()).
                                firstLogon(LocalDateTime.now()).
                                authProvider(FACEBOOK).
                                role(User.Role.ROLE_USER).
                                build();
                break;
            }
            default: {
                LOGGER.error("Unexpected oauth user type {}", provider);
                throw new IllegalArgumentException("Unexpected oauth type: " + provider);
            }
        }

        userRepository.insert(user);
        LOGGER.info("User created in repository: " + user);
        return user;
    }

    private User updateUser(Map<String, String> userDetails, User user, User.AuthenticationProvider provider) {

        switch (provider) {
            case GOOGLE: {
                user.setFirstName(userDetails.get("given_name"));
                user.setLastName(userDetails.get("family_name"));
                user.setFullName(userDetails.get("name"));
                user.setLink(userDetails.get("link"));
                user.setPicture(userDetails.get("picture"));
                user.setEmail(userDetails.get("email"));
                user.setLastLogon(LocalDateTime.now());
                break;
            }
            case FACEBOOK: {
                user.setFirstName(userDetails.get("first_name"));
                user.setLastName(userDetails.get("last_name"));
                user.setFullName(userDetails.get("name"));
                user.setLink(userDetails.get("link"));
                //user.setPicture(userDetails.get("picture.data.url"));
                user.setEmail(userDetails.get("email"));
                user.setLastLogon(LocalDateTime.now());
                break;
            }
            default: {
                LOGGER.error("Unexpected oauth user type {}", provider);
                throw new IllegalArgumentException("Unexpected oauth type: " + provider);
            }
        }

        userRepository.save(user);
        LOGGER.info("User updated in repository: " + user);
        return user;
    }
}
