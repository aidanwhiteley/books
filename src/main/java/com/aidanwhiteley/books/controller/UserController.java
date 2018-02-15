package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.dtos.ClientRoles;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.service.UserService;
import com.aidanwhiteley.books.util.AuthenticationUtils;
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;
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

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.FACEBOOK;
import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.GOOGLE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping("/secure/api")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtAuthenticationUtils authUtils;

    @Autowired
    private UserService userService;


    @RequestMapping("/user")
    @PreAuthorize("hasAnyRole('ROLE_EDITOR', 'ROLE_ADMIN', 'ROLE_USER')")
    public User user(Principal principal) {

        //Map<String, Object> userDetails = authUtils.getRemoteUserDetails(principal);

        //LOGGER.debug("Remote user details: " + userDetails);

        LOGGER.info("Principal passed in to user methoid is: " + (principal == null ? null : principal.toString()));

        User user = authUtils.extractUserFromPrincipal(principal);

        return user;
//        User.AuthenticationProvider provider = authUtils.getAuthProviderFromPrincipal(principal);
//
//        if (user == null) {
//            return userService.createUser(userDetails, provider);
//        } else {
//            return userService.updateUser(userDetails, user, provider);
//        }
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
}
