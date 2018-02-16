package com.aidanwhiteley.books.controller;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;

@RestController
@RequestMapping("/secure/api")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtAuthenticationUtils authUtils;

    @RequestMapping("/user")
    @PreAuthorize("hasAnyRole('ROLE_EDITOR', 'ROLE_ADMIN', 'ROLE_USER')")
    public User user(Principal principal) {

        LOGGER.info("Principal passed in to user method is: " + (principal == null ? null : principal.toString()));
        Optional<User> user = authUtils.extractUserFromPrincipal(principal);
        return user.isPresent() ? user.get() : null;
    }

    @RequestMapping(value = "/users", method = GET)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<User> users(Principal principal) {
        return userRepository.findAll();
    }

    @RequestMapping(value = "/users/{id}", method = DELETE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> deleteUserById(@PathVariable("id") String id, Principal principal) {

        Optional<User> user = authUtils.extractUserFromPrincipal(principal);
        if (user.isPresent()) {
            if (user.get().getId().equals(id)) {
                LOGGER.warn("User {} on {} attempted to delete themselves. This isn't allowed", user.get().getFullName(), user.get().getAuthProvider());
                return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"msg\" : \"Cant delete your own logged on user\"}");
            }

            userRepository.delete(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @RequestMapping(value = "/users/{id}", method = PATCH)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> patchUserRolesById(@PathVariable("id") String id, @RequestBody ClientRoles clientRoles, Principal principal) {

        Optional<User> user = authUtils.extractUserFromPrincipal(principal);
        if (user.isPresent()) {
            if (user.get().getId().equals(id)) {
                LOGGER.warn("User {} on {} attempted to change their own roles. This isn't allowed", user.get().getFullName(), user.get().getAuthProvider());
                return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"msg\" : \"Cant change permissions for your own logged on user\"}");
            }

            LOGGER.debug("Received patch of: {}", clientRoles);
            userRepository.updateUserRoles(clientRoles);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
