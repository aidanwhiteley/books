package com.aidanwhiteley.books.service;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.util.IntegrationTest;
import com.aidanwhiteley.books.util.Oauth2AuthenticationUtils;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class UserServiceTest extends IntegrationTest {

    private static final String DUMMY = "dummy";
    private static final String NEW_USER_1 = "New User 1";
    private static final String NEW_USER_2 = "New User 2";
    private static final String UPDATED_USER_1 = "Updated User 1";
    private static final String UPDATED_USER_2 = "Updated User 2";
    
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    
    @Autowired
    UserRepository userRepository;
    
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientClientId;
    @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
    private String facebookClientClientId;
    
    @Mock
    private OAuth2AuthenticationToken oauthToken;
    @Mock
    private OAuth2AuthorizedClientService authorisedClientService;

    @Test
    public void testCreateGoogleBasedUser() {
        testUserCreate(googleClientClientId, NEW_USER_1, User.AuthenticationProvider.GOOGLE);
    }

    @Test
    public void testCreateFacebookBasedUser() {
        testUserCreate(facebookClientClientId, NEW_USER_2, User.AuthenticationProvider.FACEBOOK);
    }

    @Test
    public void testUpdateGoogleBasedUser() {
        User user = testUserCreate(googleClientClientId, NEW_USER_1, User.AuthenticationProvider.GOOGLE);

        configureOauth(googleClientClientId, UPDATED_USER_1);
        UserService userService = configureUserService();
        User updatedUser = userService.createOrUpdateUser(oauthToken);
        assertEquals(UPDATED_USER_1, updatedUser.getFullName());

        // Check that the user was updated and not created again
        assertEquals(user.getId(), updatedUser.getId());
    }

    @Test
    public void testUpdateFacebookBasedUser() {
        User user = testUserCreate(facebookClientClientId, NEW_USER_2, User.AuthenticationProvider.FACEBOOK);

        configureOauth(facebookClientClientId, UPDATED_USER_2);
        UserService userService = configureUserService();
        User updatedUser = userService.createOrUpdateUser(oauthToken);
        assertEquals(UPDATED_USER_2, updatedUser.getFullName());

        // Check that the user was updated and not created again
        assertEquals(user.getId(), updatedUser.getId());
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
                clientSecret(DUMMY).redirectUriTemplate(DUMMY).scope(DUMMY).authorizationUri(DUMMY).tokenUri(DUMMY).
                clientName(DUMMY);
        ClientRegistration clientReg = builder.build();
        when(client.getClientRegistration()).thenReturn(clientReg);

        when(authorisedClientService.loadAuthorizedClient(any(String.class), any(String.class))).thenReturn(client);
    }
}
