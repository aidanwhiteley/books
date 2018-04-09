package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.dtos.ClientRoles;
import com.aidanwhiteley.books.controller.exceptions.AccessForbiddenException;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping("/secure/api")
public class UserController {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

	private final UserRepository userRepository;

	private final JwtAuthenticationUtils authUtils;

	private final JwtAuthenticationService authService;

	@Value("${books.client.postLogonUrl}")
	private String postLogonUrl;

	@Autowired
	public UserController(UserRepository userRepository, JwtAuthenticationUtils jwtAuthenticationUtils,
			JwtAuthenticationService jwtAuthenticationService) {
		this.userRepository = userRepository;
		this.authUtils = jwtAuthenticationUtils;
		this.authService = jwtAuthenticationService;
	}

	@RequestMapping(value = "/user", method = GET)
	public User user(Principal principal, HttpServletResponse response) {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Principal passed in to user method is: {}",
					(principal == null ? null : principal.toString()));
		}

		Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
		if (user.isPresent()) {
			return user.get();
		} else {
			// We've been supplied a valid JWT but the user is no longer in the
			// database.
			LOGGER.warn("Valid JWT passed but no corresponding user in data store");
			authService.expireJwtCookie(response);
			throw new AccessForbiddenException("No user found in user store for input JWT");
		}
	}

	@RequestMapping(value = "/users", method = GET)
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public List<User> users(Principal principal) {
		return userRepository.findAll();
	}

	@RequestMapping(value = "/users/{id}", method = DELETE)
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<Object> deleteUserById(@PathVariable("id") String id, Principal principal) {

		Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
		if (user.isPresent()) {
			if (user.get().getId().equals(id)) {
				LOGGER.warn("User {} on {} attempted to delete themselves. This isn't allowed",
						user.get().getFullName(), user.get().getAuthProvider());
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body("{\"msg\" : \"Cant delete your own logged on user\"}");
			}

			userRepository.deleteById(id);
			return ResponseEntity.ok().build();
		} else {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	@RequestMapping(value = "/users/{id}", method = PATCH)
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<Object> patchUserRolesById(@PathVariable("id") String id, @RequestBody ClientRoles clientRoles,
			Principal principal) {

		Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
		if (user.isPresent()) {
			if (user.get().getId().equals(id)) {
				LOGGER.warn("User {} on {} attempted to change their own roles. This isn't allowed",
						user.get().getFullName(), user.get().getAuthProvider());
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body("{\"msg\" : \"Cant change permissions for your own logged on user\"}");
			}

			LOGGER.debug("Received patch of: {}", clientRoles);
			userRepository.updateUserRoles(clientRoles);
			return ResponseEntity.ok().build();
		} else {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	/**
	 * A custom logout method that removes the necessary client side cookies.
	 *
	 * We dont use the Spring Security config based logout as our needs are
	 * simple and there are complexities with the ordering of Spring Security
	 * filters when we want to be able to call logout when CORS is enabled.
	 */
	@RequestMapping(value = "/logout", method = POST)
	public void logout(HttpServletResponse response) {
		authService.expireJwtCookie(response);
		authService.expireXsrfCookie(response);
		// There shouldnt be any JSessionId cookie - but kill any that exists!
		authService.expireJsessionIdCookie(response);
	}

}
