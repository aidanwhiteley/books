package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.FACEBOOK;
import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.GOOGLE;

@Component
public class Oauth2AuthenticationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Oauth2AuthenticationUtils.class);

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    private final UserRepository userRepository;

    @Value("${google.client.clientId}")
    private String googleClientClientId;

    @Value("${facebook.client.clientId}")
    private String facebookClientClientId;

    @Autowired
    public Oauth2AuthenticationUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getUserIfExists(OAuth2AuthenticationToken authentication) {

        OAuth2AuthorizedClient authorizedClient = this.getAuthorizedClient(authentication);
        String authenticationProviderId = authorizedClient.getPrincipalName();

        List<User> users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(authenticationProviderId,
                this.getAuthenticationProvider(authentication).toString());

        User user;
        switch (users.size()) {
            case 0:
                user = null;
                break;
            case 1:
                user = users.get(0);
                break;
            default:
                LOGGER.error("More than one user found for Authentication: {}", authentication);
                throw new IllegalStateException("More that one user found for a given Authentication");
        }

        return Optional.ofNullable(user);
    }

    private OAuth2AuthorizedClient getAuthorizedClient(OAuth2AuthenticationToken authentication) {
        return this.authorizedClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(), authentication.getName());
    }

	public Map<String, Object> getUserDetails(OAuth2AuthenticationToken auth) {
        LinkedHashMap<String, Object> modifiableMap = new LinkedHashMap<>();
        auth.getPrincipal().getAttributes().forEach((key, value) -> modifiableMap.put(key, value));
        return modifiableMap;
    }

    public User.AuthenticationProvider getAuthenticationProvider(OAuth2AuthenticationToken auth) {

        OAuth2AuthorizedClient authorizedClient = this.getAuthorizedClient(auth);
        String clientId = authorizedClient.getClientRegistration().getClientId();

        if (clientId.equals(googleClientClientId)) {
            return GOOGLE;
        } else if (clientId.equals(facebookClientClientId)) {
            return FACEBOOK;
        } else {
            LOGGER.error("Unknown clientId specified of {} so cant determine authentication provider. Config value is {}", clientId, googleClientClientId);
            throw new IllegalArgumentException("Uknown client id specified");
        }
    }

}
