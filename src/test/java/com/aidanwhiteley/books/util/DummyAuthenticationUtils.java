package com.aidanwhiteley.books.util;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.domain.User.AuthenticationProvider;

@Component
public class DummyAuthenticationUtils implements AuthenticationUtils {

    public static final String DUMMY_USER_FOR_TESTING_ONLY = "Dummy user for testing only";
    public static final String THIS_IS_NOT_A_REAL_AUTH_ID = "This is not a real auth id";

    @Override
    public User extractUserFromPrincipal(Principal principal) {
        return getTestUser();
    }

    public static User getTestUser() {
        User user = new User();
        user.setFullName(DUMMY_USER_FOR_TESTING_ONLY);
        user.setAuthProvider(AuthenticationProvider.GOOGLE);
        user.setFirstLogon(LocalDateTime.now());
        user.setLastLogon(LocalDateTime.now());

        user.setAuthenticationServiceId(THIS_IS_NOT_A_REAL_AUTH_ID);
        user.addRole(User.Role.ROLE_USER);
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
