package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.controller.jwt.JwtAuthentication;
import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static com.aidanwhiteley.books.util.LogDetaint.logMessageDetaint;

@Component
public class JwtAuthenticationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationUtils.class);

    private final UserRepository userRepository;

    private final UserService userService;

    private final JwtUtils jwtUtils;

    public JwtAuthenticationUtils(UserRepository userRepository, UserService userService, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.jwtUtils = jwtUtils;
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

    @Bean
    public CommandLineRunner createAndLogActuatorUserToken() {
        return args -> {
            String jwtToken = getJwtForActuatorRoleUser();
            if (!jwtToken.isEmpty()) {
                LOGGER.warn("JWT for user with just actuator role: {}", jwtToken);
            }
        };
    }

    protected String getJwtForActuatorRoleUser() {
        Optional<User> user = userService.createOrUpdateActuatorUser();
        return user.map(jwtUtils::createTokenForUser).orElse("");
    }

    private Optional<User> getUserIfExists(JwtAuthentication auth) {

        if (auth == null) {
            return Optional.empty();
        }

    	String authenticationServiceId = auth.getAuthenticationServiceId();
        String authenticationProviderId = auth.getAuthProvider();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Query user repository with service id of {} and provider of {}",
                    authenticationServiceId.replaceAll("[\n\r\t]", "_"),
                    authenticationProviderId.replaceAll("[\n\r\t]", "_"));
        }
        
        List<User> users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(authenticationServiceId, authenticationProviderId);
        User user = null;
        switch (users.size()) {
            case 0:
                break;
            case 1:
                user = users.get(0);
                break;
            default:
                handleUnexpectedAuth(auth);
        }

        return Optional.ofNullable(user);
    }

    protected static void handleUnexpectedAuth(JwtAuthentication auth) {
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error("More than one user found for JwtAuthentication: {}", logMessageDetaint(auth));
        }
        throw new IllegalStateException("More that one user found for a given Jwt Authentication");
    }

    private void checkPrincipalType(Principal principal) {
        if (!(principal instanceof JwtAuthentication)) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Only Jwt authentication currently supported and supplied Principal not Jwt: {}",
                        logMessageDetaint(principal));
            }
            throw new UnsupportedOperationException("Only Jwt principals currently supported");
        }
    }

}
