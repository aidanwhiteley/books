package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.util.AuthenticationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.GOOGLE;

@RestController
@RequestMapping("/secure/api")
@PreAuthorize("hasAnyRole('ROLE_EDITOR', 'ROLE_ADMIN', 'ROLE_USER')")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationUtils authUtils;

    @RequestMapping("/user")
    public User user(Principal principal) {

        Map<String, String> userDetails = authUtils.getRemoteUserDetails(principal);
        User user = authUtils.extractUserFromPrincipal(principal);

        if (user == null) {
            return createUserFromGoogleAuth(userDetails);
        } else {
            return updateUserFromGoogleAuth(userDetails, user);
        }
    }

    private User createUserFromGoogleAuth(Map<String, String> userDetails) {

        User googleUser = User.builder().authenticationServiceId(userDetails.get("id")).
                firstName(userDetails.get("given_name")).
                lastName(userDetails.get("family_name")).
                fullName(userDetails.get("name")).
                link(userDetails.get("link")).
                //picture(userDetails.get("picture")).
                email(userDetails.get("email")).
                lastLogon(LocalDateTime.now()).
                firstLogon(LocalDateTime.now()).
                authProvider(GOOGLE).
                role(User.Role.ROLE_USER).
                build();

        userRepository.insert(googleUser);
        LOGGER.info("User created in repository: " + googleUser);
        return googleUser;
    }

    private User updateUserFromGoogleAuth(Map<String, String> userDetails, User googleUser) {

        // In case user has made changes on Google e.g. new picture
        googleUser.setFirstName(userDetails.get("given_name"));
        googleUser.setLastName(userDetails.get("family_name"));
        googleUser.setFullName(userDetails.get("name"));
        googleUser.setLink(userDetails.get("link"));
        //googleUser.setPicture(userDetails.get("picture"));
        googleUser.setEmail(userDetails.get("email"));
        googleUser.setLastLogon(LocalDateTime.now());
        userRepository.save(googleUser);
        LOGGER.info("User updated in repository: " + googleUser);

        return googleUser;
    }
}
