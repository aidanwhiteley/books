package com.aidanwhiteley.books.service;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.util.IntegrationTest;
import com.aidanwhiteley.books.util.Oauth2AuthenticationUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.LOCAL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class UserServiceTest extends IntegrationTest {

    private static final String DUMMY = "dummy";
    private static final String NEW_USER_1 = "New User 1";
    private static final String NEW_USER_2 = "New User 2";
    private static final String UPDATED_USER_1 = "Updated User 1";
    private static final String UPDATED_USER_2 = "Updated User 2";

    @Autowired
    private UserRepository userRepository;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientClientId;
    @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
    private String facebookClientClientId;

    @Mock
    private OAuth2AuthenticationToken oauthToken;
    @Mock
    private OAuth2AuthorizedClientService authorisedClientService;

    @Test
    void testCreateGoogleBasedUser() {
        assertNotNull(testUserCreate(googleClientClientId, NEW_USER_1, User.AuthenticationProvider.GOOGLE));
    }

    @Test
    void testCreateFacebookBasedUser() {
        assertNotNull(testUserCreate(facebookClientClientId, NEW_USER_2, User.AuthenticationProvider.FACEBOOK));
    }

    @Test
    void testUpdateGoogleBasedUser() {
        User user = testUserCreate(googleClientClientId, NEW_USER_1, User.AuthenticationProvider.GOOGLE);

        configureOauth(googleClientClientId, UPDATED_USER_1);
        UserService userService = configureUserService();
        User updatedUser = userService.createOrUpdateUser(oauthToken);
        assertEquals(UPDATED_USER_1, updatedUser.getFullName());

        // Check that the user was updated and not created again
        assertEquals(user.getId(), updatedUser.getId());
    }

    @Test
    void testUpdateFacebookBasedUser() {
        User user = testUserCreate(facebookClientClientId, NEW_USER_2, User.AuthenticationProvider.FACEBOOK);

        configureOauth(facebookClientClientId, UPDATED_USER_2);
        UserService userService = configureUserService();
        User updatedUser = userService.createOrUpdateUser(oauthToken);
        assertEquals(UPDATED_USER_2, updatedUser.getFullName());

        // Check that the user was updated and not created again
        assertEquals(user.getId(), updatedUser.getId());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void testCreateActuatorUser() {
        UserService userService = configureUserService();
        userService.setAllowActuatorUserCreation(true);
        User user = userService.createOrUpdateActuatorUser().get();
        assertNotNull(user);
        String id = user.getId();
        assertEquals(LOCAL, user.getAuthProvider());

        User user2 = userService.createOrUpdateActuatorUser().get();
        String id2 = user.getId();
        assertEquals(id, id2);
        assertTrue(user2.getLastLogon().isAfter(user.getFirstLogon()), "Logon timestamp should have been updated");
    }

    @Test
    void testActuatorUserCreationOff() {
        UserService userService = new UserService(null, null);
        userService.setAllowActuatorUserCreation(false);
        Optional<User> aUser = userService.createOrUpdateActuatorUser();
        assertFalse(aUser.isPresent());
    }

    private User testUserCreate(String clientId, String name, User.AuthenticationProvider provider) {
        UserService userService = configureUserService();
        configureOauth(clientId, name);

        User user = userService.createOrUpdateUser(oauthToken);
        assertNotNull(user);
        assertEquals(provider, user.getAuthProvider());
        assertEquals(name, user.getFullName());

        return user;
    }

    private UserService configureUserService() {
        Oauth2AuthenticationUtils oauthUtils = new Oauth2AuthenticationUtils(userRepository, authorisedClientService);
        oauthUtils.setFacebookClientClientId(facebookClientClientId);
        oauthUtils.setGoogleClientClientId(googleClientClientId);
        return new UserService(userRepository, oauthUtils);
    }

    private void configureOauth(String clientId, String name) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("name", name);
        details.put(name, name);
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("USER"));

        OAuth2User oauth2User = new DefaultOAuth2User(authorities, details, name);

        when(oauthToken.getName()).thenReturn(DUMMY);
        when(oauthToken.getAuthorizedClientRegistrationId()).thenReturn(DUMMY);
        when(oauthToken.getPrincipal()).thenReturn(oauth2User);

        OAuth2AuthorizedClient client = Mockito.mock(OAuth2AuthorizedClient.class);

        ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(DUMMY);
        builder.clientId(clientId).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).
                clientSecret(DUMMY).redirectUri(DUMMY).scope(DUMMY).authorizationUri(DUMMY).tokenUri(DUMMY).
                clientName(DUMMY);
        ClientRegistration clientReg = builder.build();
        when(client.getClientRegistration()).thenReturn(clientReg);

        when(authorisedClientService.loadAuthorizedClient(any(String.class), any(String.class))).thenReturn(client);
    }
}
