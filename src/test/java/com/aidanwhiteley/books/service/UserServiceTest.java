package com.aidanwhiteley.books.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.util.IntegrationTest;

public class UserServiceTest extends IntegrationTest {

    private static final String NEW_USER_1 = "New User 1";
    private static final String NEW_USER_2 = "New User 2";
    private static final String UPDATED_USER_1 = "Updated User 1";
    private static final String UPDATED_USER_2 = "Updated User 2";

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientClientId;

    @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
    private String facebookClientClientId;

    @Autowired
    UserService userService;

    @Mock
    private OAuth2AuthenticationToken oauthToken;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void testCreateGoogleBasedUser() {
        configureOauth(googleClientClientId, NEW_USER_1);
        User user = userService.createOrUpdateUser(oauthToken);
        assertNotNull(user);
        assertEquals(User.AuthenticationProvider.GOOGLE, user.getAuthProvider());
        assertEquals(NEW_USER_1, user.getFullName());
    }

    @Test
    public void testCreateFacebookBasedUser() {
        configureOauth(facebookClientClientId, NEW_USER_2);
        User user = userService.createOrUpdateUser(oauthToken);
        assertNotNull(user);
        assertEquals(User.AuthenticationProvider.FACEBOOK, user.getAuthProvider());
        assertEquals(NEW_USER_2, user.getFullName());
    }

    @Test
    public void testUpdateGoogleBasedUser() {
        configureOauth(googleClientClientId, NEW_USER_1);
        User user = userService.createOrUpdateUser(oauthToken);
        assertEquals(NEW_USER_1, user.getFullName());

        configureOauth(googleClientClientId, UPDATED_USER_1);
        User updatedUser = userService.createOrUpdateUser(oauthToken);
        assertEquals(UPDATED_USER_1, updatedUser.getFullName());

        // Check that the user was updated and not created again
        assertEquals(user.getId(), updatedUser.getId());
    }

    @Test
    public void testUpdateFacebookBasedUser() {
        configureOauth(facebookClientClientId, NEW_USER_2);
        User user = userService.createOrUpdateUser(oauthToken);
        assertEquals(NEW_USER_2, user.getFullName());

        configureOauth(facebookClientClientId, UPDATED_USER_2);
        User updatedUser = userService.createOrUpdateUser(oauthToken);
        assertEquals(UPDATED_USER_2, updatedUser.getFullName());

        // Check that the user was updated and not created again
        assertEquals(user.getId(), updatedUser.getId());
    }

    private void configureOauth(String clientId, String name) {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("name", name);
        details.put(name, name);
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("USER"));
        
        OAuth2User oauth2User = new DefaultOAuth2User(authorities, details, name);
        
        oauthToken.setDetails(details);
        when(oauthToken.getName()).thenReturn("fred");
        when(oauthToken.getAuthorizedClientRegistrationId()).thenReturn("google");
        when(oauthToken.getPrincipal()).thenReturn(oauth2User);
        
        when(oauthToken.getDetails()).thenReturn(details);
//        when(oauth2User.getAttributes()).thenReturn(details);
//        when(oauth.getUserAuthentication()).thenReturn(auth);
//        when(oauth.getOAuth2Request()).thenReturn(storedRquest);
//        when(storedRquest.getClientId()).thenReturn(clientId);
    }
}
