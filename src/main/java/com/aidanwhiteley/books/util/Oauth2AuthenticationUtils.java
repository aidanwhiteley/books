package com.aidanwhiteley.books.util;

import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.FACEBOOK;
import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.GOOGLE;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.stereotype.Component;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;

@Component
public class Oauth2AuthenticationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Oauth2AuthenticationUtils.class);

    private final UserRepository userRepository;

    @Value("${google.client.clientId}")
    private String googleClientClientId;

    @Value("${facebook.client.clientId}")
    private String facebookClientClientId;

    @Autowired
    public Oauth2AuthenticationUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getUserIfExists(OAuth2Authentication auth) {

        String authenticationProviderId = (String) auth.getUserAuthentication().getPrincipal();
        List<User> users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(authenticationProviderId,
                this.getAuthenticationProvider(auth).toString());

        User user;
        switch (users.size()) {
            case 0:
                user = null;
                break;
            case 1:
                user = users.get(0);
                break;
            default:
                LOGGER.error("More than one user found for Authentication: {}", auth);
                throw new IllegalStateException("More that one user found for a given Authentication");
        }

        return Optional.ofNullable(user);
    }

    @SuppressWarnings("unchecked")
	public Map<String, Object> getUserDetails(OAuth2Authentication auth) {
        return (LinkedHashMap<String, Object>) auth.getUserAuthentication().getDetails();
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

}
