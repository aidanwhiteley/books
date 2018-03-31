package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.controller.jwt.JwtAuthentication;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationUtils.class);

    private final UserRepository userRepository;

    @Autowired
    public JwtAuthenticationUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns a User object by processing a supplied JWT.
     *
     * The caller is allowed to specify whether they want the data store queried for the
     * current state of the user data or whether they just want to use data from the JWT.
     *
     * @param principal The user principal
     * @param useTokenOnly Whether to just look at the data in the JWT or query the data store
     * @return A User object - will only be partially populated if using only JWT data.
     */
    public Optional<User> extractUserFromPrincipal(Principal principal, boolean useTokenOnly) {

        if (null == principal) {
            return Optional.empty();
        }

        checkPrincipalType(principal);

        JwtAuthentication auth = (JwtAuthentication) principal;

        if (useTokenOnly) {
            return Optional.of(auth.getUser());
        } else {
            return getUserIfExists(auth);
        }
    }

    private Optional<User> getUserIfExists(JwtAuthentication auth) {

        if (auth == null) {
            return Optional.empty();
        }

    	String authenticationServiceId = auth.getAuthenticationServiceId();
        String authenticationProviderId = auth.getAuthProvider();
        
        LOGGER.debug("Query user repository with id of {} and provider of {}", authenticationServiceId, authenticationProviderId);
        
        List<User> users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(authenticationServiceId, authenticationProviderId);
        User user;
        switch (users.size()) {
            case 0:
                user = null;
                break;
            case 1:
                user = users.get(0);
                break;
            default:
                LOGGER.error("More than one user found for JwtAuthentication: {}", auth);
                throw new IllegalStateException("More that one user found for a given Jwt Authentication");
        }

        return Optional.ofNullable(user);
    }

    private void checkPrincipalType(Principal principal) {
        if (!(principal instanceof JwtAuthentication)) {
            LOGGER.error("Only Jwt authentication currently supported and supplied Principal not Jwt: {}", principal);
            throw new UnsupportedOperationException("Only Jwt principals currently supported");
        }
    }


}
