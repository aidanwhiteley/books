package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.dtos.ClientRoles;
import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService.JSESSIONID_COOKIE_NAME;
import static com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService.JWT_COOKIE_NAME;
import static com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService.XSRF_COOKIE_NAME;
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
    private JwtAuthenticationService authService;

    @Value("${books.client.postLogonUrl}")
    private String postLogonUrl;

    @RequestMapping("/user")
    @PreAuthorize("hasAnyRole('ROLE_EDITOR', 'ROLE_ADMIN', 'ROLE_USER')")
    public User user(Principal principal) {

        LOGGER.info("Principal passed in to user method is: " + (principal == null ? null : principal.toString()));
        Optional<User> user = authUtils.extractUserFromPrincipal(principal);
        if (user.isPresent()) {
            return user.get();
        } else {
            // We've been supplied a valid JWT but the user is no longer in the database.
            LOGGER.warn("Valid JWT passed but no corresponding user in data store");
            // TODO - remove the JWT cookie
            throw new IllegalArgumentException();
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

    @RequestMapping(value = "/logout", method = POST)
    public LogoutInfo logout(HttpServletResponse response)  {
        authService.expireJwtCookie(response);
        authService.expireXsrfCookie(response);

        // There should be no http session but this cookie is being set at the moment (for some reason).
        authService.expireJsessionIdfCookie(response);

        return new LogoutInfo();
    }

    @SuppressWarnings("serial")
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    class IllegalArgumentException extends RuntimeException {
    }

    class LogoutInfo {

        private boolean loggedOut = true;
        private String redirectUrl = postLogonUrl;

        public boolean isLoggedOut() {
            return loggedOut;
        }

        public String getRedirectUrl() {
            return redirectUrl;
        }
    }

}
