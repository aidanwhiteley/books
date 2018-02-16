package com.aidanwhiteley.books.util;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.aidanwhiteley.books.controller.jwt.JwtAuthentication;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;

@Component
@Profile({"!integration"})
public class JwtAuthenticationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationUtils.class);

    @Autowired
    private UserRepository userRepository;


    public Optional<User> extractUserFromPrincipal(Principal principal) {

        if (null == principal) {
            return Optional.ofNullable(null);
        }

        checkPrincipalType(principal);

        JwtAuthentication auth = (JwtAuthentication) principal;
        return getUserIfExists(auth);
    }

    public Optional<User> getUserIfExists(JwtAuthentication auth) {

        if (auth == null) {
            return Optional.ofNullable(null);
        }

    	String authenticationServiceId = auth.getAuthenticationServiceId();
        String authenticationProviderId = auth.getAuthProvider();
        
        LOGGER.debug("Query user repository with id of {} and provider of {}", authenticationServiceId, authenticationProviderId);
        
        List<User> users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(authenticationServiceId, authenticationProviderId);

        User user;
        if (users.size() == 0) {
            user = null;
        } else if (users.size() == 1) {
            user = users.get(0);
        } else {
            LOGGER.error("More than one user found for Authentication: {}", auth);
            throw new IllegalStateException("More that one user found for a given Authentication");
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
