package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.domain.User;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

public interface AuthenticationUtils {

    User extractUserFromPrincipal(Principal principal);
    Map<String, Object> getRemoteUserDetails(Principal principal);
    User.AuthenticationProvider getAuthProviderFromPrincipal(Principal principal);
    String getAuthProviderFromPrincipalAsString(Principal principal);

    Optional<User> getUserIfExists(OAuth2Authentication auth);
    Map<String, Object> getUserDetails(OAuth2Authentication auth);
    User.AuthenticationProvider getAuthenticationProvider(OAuth2Authentication auth);
}
