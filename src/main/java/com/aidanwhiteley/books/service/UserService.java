package com.aidanwhiteley.books.service;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.util.Oauth2AuthenticationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.FACEBOOK;
import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.GOOGLE;

@Service
public class UserService {

    private static final String EMAIL = "email";
    private static final String PICTURE = "picture";

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final Oauth2AuthenticationUtils authUtils;
    
    @Value("${books.users.default.admin.email}")
    private String defaultAdminEmail;

    @Autowired
    public UserService(UserRepository userRepository, Oauth2AuthenticationUtils oauth2AuthenticationUtils) {
        this.userRepository = userRepository;
        this.authUtils = oauth2AuthenticationUtils;
    }

    public User createOrUpdateUser(OAuth2AuthenticationToken authentication) {

        Map<String, Object> userDetails = authUtils.getUserDetails(authentication);
        User.AuthenticationProvider provider = authUtils.getAuthenticationProvider(authentication);
        Optional<User> user = authUtils.getUserIfExists(authentication);
        return user.map(user1 -> updateUser(userDetails, user1, provider)).orElseGet(() -> createUser(userDetails, provider));
    }

    private User createUser(Map<String, Object> userDetails, User.AuthenticationProvider provider) {

        User user;
        LocalDateTime now = LocalDateTime.now();

        switch (provider) {
            case GOOGLE: {
                user = createGoogleUser(userDetails, now);
                break;
            }
            case FACEBOOK: {
                user = createFacebookUser(userDetails);
                break;
            }
            default: {
                LOGGER.error("Unexpected oauth user type {}", provider);
                throw new IllegalArgumentException("Unexpected oauth type: " + provider);
            }
        }

        userRepository.insert(user);
        LOGGER.info("User created in repository: {}", user);
        return user;
    }

    private User createFacebookUser(Map<String, Object> userDetails) {
        User user;
        user = User.builder().authenticationServiceId((String) userDetails.get("id")).
                firstName((String) userDetails.get("first_name")).
                lastName((String) userDetails.get("last_name")).
                fullName((String) userDetails.get("name")).
                link((String) userDetails.get("link")).
                email((String) userDetails.get(EMAIL)).
                lastLogon(LocalDateTime.now()).
                firstLogon(LocalDateTime.now()).
                authProvider(FACEBOOK).
                build();
        user = setDefaultAdminUser(user);
        user.addRole(User.Role.ROLE_USER);

        String url = extractFaceBookPictureUrl(userDetails);
        if (url != null) {
            user.setPicture(url);
        }
        return user;
    }

    private User createGoogleUser(Map<String, Object> userDetails, LocalDateTime now) {
        User user;
        user = User.builder().authenticationServiceId((String) userDetails.get("sub")).
                firstName((String) userDetails.get("given_name")).
                lastName((String) userDetails.get("family_name")).
                fullName((String) userDetails.get("name")).
                link((String) userDetails.get("link")).
                picture((String) userDetails.get(PICTURE)).
                email((String) userDetails.get(EMAIL)).
                lastLogon(now).
                firstLogon(now).
                authProvider(GOOGLE).
                build();

        user = setDefaultAdminUser(user);
        user.addRole(User.Role.ROLE_USER);
        return user;
    }

    private User updateUser(Map<String, Object> userDetails, User user, User.AuthenticationProvider provider) {

        switch (provider) {
            case GOOGLE: {
                updateGoogleUser(userDetails, user);
                break;
            }
            case FACEBOOK: {
                updateFacebookUser(userDetails, user);
                break;
            }
            default: {
                LOGGER.error("Unexpected oauth user type {}", provider);
                throw new IllegalArgumentException("Unexpected oauth type: " + provider);
            }
        }

        userRepository.save(user);
        LOGGER.info("User updated in repository: {}", user);
        return user;
    }

    private void updateFacebookUser(Map<String, Object> userDetails, User user) {
        user.setFirstName((String) userDetails.get("first_name"));
        user.setLastName((String) userDetails.get("last_name"));
        user.setFullName((String) userDetails.get("name"));
        user.setLink((String) userDetails.get("link"));
        String url = extractFaceBookPictureUrl(userDetails);
        if (url != null) {
            user.setPicture(url);
        }
        user.setEmail((String) userDetails.get(EMAIL));
        user.setLastLogon(LocalDateTime.now());
    }

    private void updateGoogleUser(Map<String, Object> userDetails, User user) {
        user.setFirstName((String) userDetails.get("given_name"));
        user.setLastName((String) userDetails.get("family_name"));
        user.setFullName((String) userDetails.get("name"));
        user.setLink((String) userDetails.get("link"));
        user.setPicture((String) userDetails.get(PICTURE));
        user.setEmail((String) userDetails.get(EMAIL));
        user.setLastLogon(LocalDateTime.now());
    }

    private User setDefaultAdminUser(User user) {
        if (defaultAdminEmail != null && defaultAdminEmail.equals(user.getEmail())) {
            user.addRole(User.Role.ROLE_EDITOR);
            user.addRole(User.Role.ROLE_ADMIN);
        }

        return user;
    }

    private String extractFaceBookPictureUrl(Map<String, Object> userDetails) {
        if (userDetails.get(PICTURE) != null && userDetails.get(PICTURE) instanceof LinkedHashMap) {
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> picture = (LinkedHashMap<String, Object>) userDetails.get(PICTURE);
            if (picture.get("data") != null && picture.get("data") instanceof LinkedHashMap) {
                @SuppressWarnings("unchecked")
                LinkedHashMap<String, Object> data = (LinkedHashMap<String, Object>) picture.get("data");
                return (String) data.get("url");
            }
        }
        return null;
    }
}
