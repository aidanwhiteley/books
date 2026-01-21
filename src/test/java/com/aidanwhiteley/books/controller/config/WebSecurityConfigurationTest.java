package com.aidanwhiteley.books.controller.config;

import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationFilter;
import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.service.UserService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSecurityConfigurationTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtAuthenticationService jwtAuthenticationService;

    @Mock
    private UserService userService;

    private WebSecurityConfiguration webSecurityConfiguration;

    private static final String POST_LOGON_URL = "/books";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_NAME = "Test User";
    private static final String REGISTRATION_ID = "google";

    @BeforeEach
    void setUp() {
        webSecurityConfiguration = new WebSecurityConfiguration(
                jwtAuthenticationFilter,
                jwtAuthenticationService,
                userService
        );
        // Set the postLogonUrl field that would normally be injected by @Value
        ReflectionTestUtils.setField(webSecurityConfiguration, "postLogonUrl", POST_LOGON_URL);
    }

    @Test
    void testOnAuthenticationSuccess() throws IOException, ServletException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        OAuth2AuthenticationToken authentication = createOAuth2Authentication(
                TEST_EMAIL, TEST_NAME, "12345", "sub", REGISTRATION_ID);

        User testUser = createUser("12345", TEST_EMAIL, TEST_NAME,
                "Test", "User", User.AuthenticationProvider.GOOGLE);

        setupMocksForAuthenticationSuccess(testUser);

        // When
        executeAuthenticationSuccess(request, response, authentication);

        // Then
        verifyAuthenticationSuccessInteractions(authentication, testUser, response);
        assertEquals(POST_LOGON_URL, response.getRedirectedUrl(),
                "Should redirect to post logon URL");
    }

    @Test
    void testOnAuthenticationSuccessWithDifferentProvider() throws IOException, ServletException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        OAuth2AuthenticationToken authentication = createOAuth2AuthenticationWithCustomAttributes(
                "facebook@example.com", "Facebook User", "fb123", "id", "facebook");

        User testUser = createUser("fb123", "facebook@example.com", "Facebook User",
                null, null, User.AuthenticationProvider.FACEBOOK);

        setupMocksForAuthenticationSuccess(testUser);

        // When
        executeAuthenticationSuccess(request, response, authentication);

        // Then
        verifyAuthenticationSuccessInteractions(authentication, testUser, response);
        assertEquals(POST_LOGON_URL, response.getRedirectedUrl(),
                "Should redirect to post logon URL");
    }

    @Test
    void testOnAuthenticationSuccessVerifiesUserServiceInteraction() throws IOException, ServletException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        OAuth2AuthenticationToken authentication = createOAuth2Authentication(
                TEST_EMAIL, null, "oauth-id-123", "sub", REGISTRATION_ID);

        User returnedUser = createUser("oauth-id-123", TEST_EMAIL, null,
                null, null, User.AuthenticationProvider.GOOGLE);

        when(userService.createOrUpdateUser(authentication)).thenReturn(returnedUser);
        doNothing().when(jwtAuthenticationService).setAuthenticationData(response, returnedUser);

        // When
        executeAuthenticationSuccess(request, response, authentication);

        // Then - verify the exact user object returned from userService is passed to jwtAuthenticationService
        verify(jwtAuthenticationService).setAuthenticationData(response, returnedUser);
    }

    // Helper methods

    private OAuth2AuthenticationToken createOAuth2Authentication(
            String email, String name, String id, String nameAttributeKey, String registrationId) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", email);
        if (name != null) {
            attributes.put("name", name);
        }
        attributes.put(nameAttributeKey, id);

        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                nameAttributeKey
        );

        return new OAuth2AuthenticationToken(
                oauth2User,
                Collections.emptyList(),
                registrationId
        );
    }

    private OAuth2AuthenticationToken createOAuth2AuthenticationWithCustomAttributes(
            String email, String name, String id, String nameAttributeKey, String registrationId) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", email);
        attributes.put("name", name);
        attributes.put(nameAttributeKey, id);

        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                nameAttributeKey
        );

        return new OAuth2AuthenticationToken(
                oauth2User,
                Collections.emptyList(),
                registrationId
        );
    }

    private User createUser(String authServiceId, String email, String fullName,
                           String firstName, String lastName, User.AuthenticationProvider provider) {
        User.UserBuilder builder = User.builder()
                .authenticationServiceId(authServiceId)
                .email(email)
                .authProvider(provider)
                .roles(Collections.singletonList(User.Role.ROLE_USER))
                .firstLogon(LocalDateTime.now())
                .lastLogon(LocalDateTime.now());

        if (fullName != null) {
            builder.fullName(fullName);
        }
        if (firstName != null) {
            builder.firstName(firstName);
        }
        if (lastName != null) {
            builder.lastName(lastName);
        }

        return builder.build();
    }

    private void setupMocksForAuthenticationSuccess(User testUser) {
        when(userService.createOrUpdateUser(any(OAuth2AuthenticationToken.class)))
                .thenReturn(testUser);
        doNothing().when(jwtAuthenticationService).setAuthenticationData(any(), any());
    }

    private void executeAuthenticationSuccess(MockHttpServletRequest request,
                                              MockHttpServletResponse response,
                                              OAuth2AuthenticationToken authentication)
            throws IOException, ServletException {
        WebSecurityConfiguration.Oauth2AuthenticationSuccessHandler handler =
                webSecurityConfiguration.new Oauth2AuthenticationSuccessHandler();
        handler.onAuthenticationSuccess(request, response, authentication);
    }

    private void verifyAuthenticationSuccessInteractions(OAuth2AuthenticationToken authentication,
                                                         User testUser,
                                                         MockHttpServletResponse response) {
        verify(userService, times(1)).createOrUpdateUser(authentication);
        verify(jwtAuthenticationService, times(1)).setAuthenticationData(response, testUser);
    }
}
