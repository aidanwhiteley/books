//package com.aidanwhiteley.books.service;
//
//import com.aidanwhiteley.books.domain.User;
//import com.aidanwhiteley.books.util.IntegrationTest;
//import org.junit.Rule;
//import org.junit.Test;
//import org.mockito.Mock;
//import org.mockito.junit.MockitoJUnit;
//import org.mockito.junit.MockitoRule;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.core.Authentication;
//
//import java.util.LinkedHashMap;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//import static org.mockito.Mockito.when;
//
//public class UserServiceTest extends IntegrationTest {
//
//    private static final String NEW_USER_1 = "New User 1";
//    private static final String NEW_USER_2 = "New User 2";
//    private static final String UPDATED_USER_1 = "Updated User 1";
//    private static final String UPDATED_USER_2 = "Updated User 2";
//
//    @Value("${google.client.clientId}")
//    private String googleClientClientId;
//
//    @Value("${facebook.client.clientId}")
//    private String facebookClientClientId;
//
//    @Autowired
//    UserService userService;
//
//    @Mock
//    private OAuth2Authentication oauth;
//    @Mock
//    private Authentication auth;
//    @Mock
//    private OAuth2Request storedRquest;
//
//    @Rule
//    public MockitoRule mockitoRule = MockitoJUnit.rule();
//
//    @Test
//    public void testCreateGoogleBasedUser() {
//        configureOauth(googleClientClientId, NEW_USER_1);
//        User user = userService.createOrUpdateUser(oauth);
//        assertNotNull(user);
//        assertEquals(User.AuthenticationProvider.GOOGLE, user.getAuthProvider());
//        assertEquals(NEW_USER_1, user.getFullName());
//    }
//
//    @Test
//    public void testCreateFacebookBasedUser() {
//        configureOauth(facebookClientClientId, NEW_USER_2);
//        User user = userService.createOrUpdateUser(oauth);
//        assertNotNull(user);
//        assertEquals(User.AuthenticationProvider.FACEBOOK, user.getAuthProvider());
//        assertEquals(NEW_USER_2, user.getFullName());
//    }
//
//    @Test
//    public void testUpdateGoogleBasedUser() {
//        configureOauth(googleClientClientId, NEW_USER_1);
//        User user = userService.createOrUpdateUser(oauth);
//        assertEquals(NEW_USER_1, user.getFullName());
//
//        configureOauth(googleClientClientId, UPDATED_USER_1);
//        User updatedUser = userService.createOrUpdateUser(oauth);
//        assertEquals(UPDATED_USER_1, updatedUser.getFullName());
//
//        // Check that the user was updated and not created again
//        assertEquals(user.getId(), updatedUser.getId());
//    }
//
//    @Test
//    public void testUpdateFacebookBasedUser() {
//        configureOauth(facebookClientClientId, NEW_USER_2);
//        User user = userService.createOrUpdateUser(oauth);
//        assertEquals(NEW_USER_2, user.getFullName());
//
//        configureOauth(facebookClientClientId, UPDATED_USER_2);
//        User updatedUser = userService.createOrUpdateUser(oauth);
//        assertEquals(UPDATED_USER_2, updatedUser.getFullName());
//
//        // Check that the user was updated and not created again
//        assertEquals(user.getId(), updatedUser.getId());
//    }
//
//    private void configureOauth(String clientId, String name) {
//        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
//        details.put("name", name);
//        when(auth.getDetails()).thenReturn(details);
//        when(oauth.getUserAuthentication()).thenReturn(auth);
//        when(oauth.getOAuth2Request()).thenReturn(storedRquest);
//        when(storedRquest.getClientId()).thenReturn(clientId);
//    }
//}
