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

import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.FACEBOOK;
import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.GOOGLE;

@Component
@Profile("!" + "integration")
public class OauthAuthenticationUtils implements AuthenticationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(OauthAuthenticationUtils.class);

    @Autowired
    private UserRepository userRepository;

    @Value("${google.client.clientId}")
    private String googleClientClientId;

    @Value("${facebook.client.clientId}")
    private String facebookClientClientId;

    public User extractUserFromPrincipal(Principal principal) {

        checkPrincipalType(principal);

        OAuth2Authentication auth = (OAuth2Authentication) principal;
        String authenticationProviderId = (String) auth.getUserAuthentication().getPrincipal();
        List<User> users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(authenticationProviderId,
                getAuthProviderFromAuthAsString(auth));

        if (users.size() == 0) {
            return null;
        } else if (users.size() == 1) {
            return users.get(0);
        } else {
            LOGGER.error("More than one user found for principcal: {}", principal);
            throw new IllegalStateException("More that one user found for a given Principal");
        }
    }

    public Map<String, String> getRemoteUserDetails(Principal principal) {

        checkPrincipalType(principal);

        OAuth2Authentication auth = (OAuth2Authentication) principal;
        @SuppressWarnings("unchecked")
        Map<String, String> userDetails = (LinkedHashMap) auth.getUserAuthentication().getDetails();

        return userDetails;
    }

    private User.AuthenticationProvider getAuthProviderFromAuth(OAuth2Authentication auth) {
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

    private void checkPrincipalType(Principal principal) {
        if (!(principal instanceof OAuth2Authentication)) {
            LOGGER.error("Only OAuth authentication currently supported and supplied Principal not ouath: {}", principal);
            throw new UnsupportedOperationException("Only OAuth principals currently supported");
        }
    }

    private String getAuthProviderFromAuthAsString(OAuth2Authentication auth) {
        return this.getAuthProviderFromAuth(auth).toString();
    }
}
