package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.domain.User;

import java.security.Principal;
import java.util.Map;

public interface AuthenticationUtils {

    User extractUserFromPrincipal(Principal principal);
    Map<String, String> getRemoteUserDetails(Principal principal);
}
