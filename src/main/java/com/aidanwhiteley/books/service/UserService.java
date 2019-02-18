package com.aidanwhiteley.books.service;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.util.Oauth2AuthenticationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.*;

@Service
public class UserService {

    private static final String EMAIL = "email";
    private static final String PICTURE = "picture";

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private static final String LOCAL_ACTUATOR_USER = "LOCAL_ACTUATOR_USER";
    private static final String FIRST_NAME_PROPERTY = "first_name";
    private static final String LAST_NAME_PROPERTY = "last_name";
    private static final String NAME_PROPERTY = "name";

    private final UserRepository userRepository;
    private final Oauth2AuthenticationUtils authUtils;
    
    @Value("${books.users.default.admin.email}")
    private String defaultAdminEmail;

    @Value("${books.users.allow.actuator.user.creation}")
    private boolean allowActuatorUserCreation;

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

    public User createOrUpdateActuatorUser() {

        if (allowActuatorUserCreation) {
            Map<String, Object> userDetails = new HashMap<>();
            User.AuthenticationProvider provider = LOCAL;
            userDetails.put(FIRST_NAME_PROPERTY, "Actuator");
            userDetails.put(LAST_NAME_PROPERTY, "User");
            userDetails.put(NAME_PROPERTY, "Actuator User");

            List<User> users = userRepository.
                    findAllByAuthenticationServiceIdAndAuthProvider(LOCAL_ACTUATOR_USER, LOCAL.toString());
            if (users.isEmpty()) {
                return createUser(userDetails, provider);
            } else {
                return updateUser(userDetails, users.get(0), provider);
            }
        } else {
            throw new UnsupportedOperationException("Creation of JWT token for accessing Actuator end points not supported");
        }
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
            case LOCAL: {
                user = createLocalActuatorUser(userDetails, now);
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
                firstName((String) userDetails.get(FIRST_NAME_PROPERTY)).
                lastName((String) userDetails.get(LAST_NAME_PROPERTY)).
                fullName((String) userDetails.get(NAME_PROPERTY)).
                link((String) userDetails.get("link")).
                email((String) userDetails.get(EMAIL)).
                lastLogon(LocalDateTime.now()).
                firstLogon(LocalDateTime.now()).
                authProvider(FACEBOOK).
                build();
        setDefaultAdminUser(user);
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
                fullName((String) userDetails.get(NAME_PROPERTY)).
                link((String) userDetails.get("link")).
                picture((String) userDetails.get(PICTURE)).
                email((String) userDetails.get(EMAIL)).
                lastLogon(now).
                firstLogon(now).
                authProvider(GOOGLE).
                build();

        setDefaultAdminUser(user);
        user.addRole(User.Role.ROLE_USER);
        return user;
    }

    private User createLocalActuatorUser(Map<String, Object> userDetails, LocalDateTime now) {
        User user;
        user = User.builder().authenticationServiceId(LOCAL_ACTUATOR_USER).
                firstName((String) userDetails.get(FIRST_NAME_PROPERTY)).
                lastName((String) userDetails.get(LAST_NAME_PROPERTY)).
                fullName((String) userDetails.get(NAME_PROPERTY)).
                lastLogon(now).
                firstLogon(now).
                authProvider(LOCAL).
                build();

        user.addRole(User.Role.ROLE_ACTUATOR);
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
            case LOCAL: {
                updateLocalActuatorUser(user);
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
        user.setFirstName((String) userDetails.get(FIRST_NAME_PROPERTY));
        user.setLastName((String) userDetails.get(LAST_NAME_PROPERTY));
        user.setFullName((String) userDetails.get(NAME_PROPERTY));
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
        user.setFullName((String) userDetails.get(NAME_PROPERTY));
        user.setLink((String) userDetails.get("link"));
        user.setPicture((String) userDetails.get(PICTURE));
        user.setEmail((String) userDetails.get(EMAIL));
        user.setLastLogon(LocalDateTime.now());
    }

    private void updateLocalActuatorUser(User user) {
        user.setLastLogon(LocalDateTime.now());
    }

    private void setDefaultAdminUser(User user) {
        if (defaultAdminEmail != null && defaultAdminEmail.equals(user.getEmail())) {
            user.addRole(User.Role.ROLE_EDITOR);
            user.addRole(User.Role.ROLE_ACTUATOR);
            user.addRole(User.Role.ROLE_ADMIN);
        }
    }

    private String extractFaceBookPictureUrl(Map<String, Object> userDetails) {
        if (userDetails.get(PICTURE) instanceof LinkedHashMap) {
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> picture = (LinkedHashMap<String, Object>) userDetails.get(PICTURE);
            if (picture.get("data") instanceof LinkedHashMap) {
                @SuppressWarnings("unchecked")
                LinkedHashMap<String, Object> data = (LinkedHashMap<String, Object>) picture.get("data");
                return (String) data.get("url");
            }
        }
        return null;
    }
}
