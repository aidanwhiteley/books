package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.domain.User;

import java.security.Principal;
import java.util.Map;

public interface AuthenticationUtils {

    User extractUserFromPrincipal(Principal principal);

    Map<String, Object> getRemoteUserDetails(Principal principal);
    User.AuthenticationProvider getAuthProviderFromPrincipal(Principal principal);
    String getAuthProviderFromPrincipalAsString(Principal principal);
    User.Role getUsersHighestRole(Principal principal);
}
