package com.aidanwhiteley.books.controller;

import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.FACEBOOK;
import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.GOOGLE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aidanwhiteley.books.controller.dtos.ClientRoles;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.util.AuthenticationUtils;

@RestController
@RequestMapping("/secure/api")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Value("${books.users.default.admin.authenticationServiceId}")
    private String defaultAdminAuthenticationServiceId;

    @Value("${books.users.default.admin.authProvider}")
    private String defaultAdminAuthProvider;

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
    public ResponseEntity<?> deleteUserById(@PathVariable("id") String id, Principal principal) {

        User user = authUtils.extractUserFromPrincipal(principal);
        if (user.getId().equals(id)) {
            LOGGER.warn("User {} on {} attempted to delete themselves. This isn't allowed", user.getFullName(), user.getAuthProvider());
            return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"msg\" : \"Cant delete your own logged on user\"}");
        }

        userRepository.delete(id);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/users/{id}", method = PATCH)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> patchUserRolesById(@PathVariable("id") String id, @RequestBody ClientRoles clientRoles, Principal principal) {

        User user = authUtils.extractUserFromPrincipal(principal);
        if (user.getId().equals(id)) {
            LOGGER.warn("User {} on {} attempted to change their own roles. This isn't allowed", user.getFullName(), user.getAuthProvider());
            return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"msg\" : \"Cant change permissions for your own logged on user\"}");
        }

        LOGGER.debug("Received patch of: {}", clientRoles);
        userRepository.updateUserRoles(clientRoles);
        return ResponseEntity.ok().build();
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
                        build();

                setDefaultAdminUser(userDetails, user, GOOGLE);
                user.addRole(User.Role.ROLE_USER);
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
                        build();
                setDefaultAdminUser(userDetails, user, FACEBOOK);
                user.addRole(User.Role.ROLE_USER);
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

    private void setDefaultAdminUser(Map<String, String> userDetails, User user, User.AuthenticationProvider provider) {
        if (defaultAdminAuthenticationServiceId != null && defaultAdminAuthenticationServiceId.length() > 0
                && defaultAdminAuthenticationServiceId.equals(userDetails.get("id")) && defaultAdminAuthProvider != null
                && defaultAdminAuthProvider.equals(provider.toString())) {
            user.addRole(User.Role.ROLE_EDITOR);
            user.addRole(User.Role.ROLE_ADMIN);
        }
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
