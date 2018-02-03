package com.aidanwhiteley.books.util;

import java.security.Principal;
import java.util.Map;

import com.aidanwhiteley.books.domain.User;

public interface AuthenticationUtils {

    User extractUserFromPrincipal(Principal principal);
    Map<String, String> getRemoteUserDetails(Principal principal);
    User.AuthenticationProvider getAuthProviderFromPrincipal(Principal principal);
    String getAuthProviderFromPrincipalAsString(Principal principal);
    User.Role getUsersHighestRole(Principal principal);
}
