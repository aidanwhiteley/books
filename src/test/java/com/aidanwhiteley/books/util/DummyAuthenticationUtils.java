package com.aidanwhiteley.books.util;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.domain.User.AuthenticationProvider;

@Component
public class DummyAuthenticationUtils implements AuthenticationUtils {
    @Override
    public User extractUserFromPrincipal(Principal principal) {
        User user = new User();
        user.setFullName("Dummy user for testing only");
        user.setAuthProvider(User.AuthenticationProvider.GOOGLE);
        user.setFirstLogon(LocalDateTime.now());
        user.setLastLogon(LocalDateTime.now());

        user.setAuthenticationServiceId("This is not a real auth id");
        List<User.Role> roles = new ArrayList<User.Role>();
        roles.add(User.Role.ROLE_USER);
        user.setRoles(roles);
        return user;
    }

    @Override
    public Map<String, String> getRemoteUserDetails(Principal principal) {
        return new LinkedHashMap<String, String>();
    }

	@Override
	public AuthenticationProvider getAuthProviderFromPrincipal(Principal principal) {
		return User.AuthenticationProvider.GOOGLE;
	}

	@Override
	public String getAuthProviderFromPrincipalAsString(Principal principal) {
		return User.AuthenticationProvider.GOOGLE.toString();
	}
}
