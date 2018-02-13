package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.FACEBOOK;
import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.GOOGLE;

@Component
@Profile({"!integration"})
public class Oauth2AuthenticationUtils implements AuthenticationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Oauth2AuthenticationUtils.class);

    @Autowired
    private UserRepository userRepository;

    @Value("${google.client.clientId}")
    private String googleClientClientId;

    @Value("${facebook.client.clientId}")
    private String facebookClientClientId;

    @Override
    public User extractUserFromPrincipal(Principal principal) {

        if (null == principal) {
            return null;
        }

        checkPrincipalType(principal);

        OAuth2Authentication auth = (OAuth2Authentication) principal;
        Optional<User> user = getUserIfExists(auth);

        if (user.isPresent()) {
            return user.get();
        } else {
            return null;
        }
    }

    public Optional<User> getUserIfExists(OAuth2Authentication auth) {

        String authenticationProviderId = (String) auth.getUserAuthentication().getPrincipal();
        List<User> users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(authenticationProviderId,
                this.getAuthenticationProvider(auth).toString());

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

    @Override
    public Map<String, Object> getRemoteUserDetails(Principal principal) {

        checkPrincipalType(principal);
        OAuth2Authentication auth = (OAuth2Authentication) principal;
        return getUserDetails(auth);
    }

    public Map<String, Object> getUserDetails(OAuth2Authentication auth) {
        return (LinkedHashMap<String, Object>) auth.getUserAuthentication().getDetails();
    }

    @Override
    public User.AuthenticationProvider getAuthProviderFromPrincipal(Principal principal) {
    	
    	checkPrincipalType(principal);
    	OAuth2Authentication auth = (OAuth2Authentication) principal;
        return getAuthenticationProvider(auth);
    }

    public User.AuthenticationProvider getAuthenticationProvider(OAuth2Authentication auth) {
        OAuth2Request storedRquest = auth.getOAuth2Request();
        String clientId = storedRquest.getClientId();

        if (clientId.equals(googleClientClientId)) {
            return GOOGLE;
        } else if (clientId.equals(facebookClientClientId)) {
            return FACEBOOK;
        } else {
            LOGGER.error("Unknown clientId specified of {} so cant determine authentication provider. Config value is {}", clientId, googleClientClientId);
            throw new IllegalArgumentException("Uknown client id specified");
        }
    }

    @Override
    public String getAuthProviderFromPrincipalAsString(Principal principal) {
        return this.getAuthProviderFromPrincipal(principal).toString();
    }

    private void checkPrincipalType(Principal principal) {
        if (!(principal instanceof OAuth2Authentication)) {
            LOGGER.error("Only OAuth authentication currently supported and supplied Principal not oauth: {}", principal);
            throw new UnsupportedOperationException("Only OAuth principals currently supported");
        }
    }


}
