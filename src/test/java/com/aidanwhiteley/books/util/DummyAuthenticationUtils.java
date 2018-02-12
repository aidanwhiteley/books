package com.aidanwhiteley.books.util;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.domain.User.AuthenticationProvider;
import com.aidanwhiteley.books.domain.User.Role;

@Component
public class DummyAuthenticationUtils implements AuthenticationUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DummyAuthenticationUtils.class);

    public static final String DUMMY_USER_FOR_TESTING_ONLY = "Dummy user for testing only";
    public static final String THIS_IS_NOT_A_REAL_AUTH_ID = "This is not a real auth id";
    public static final String DUMMY_EMAIL = "example@example.com";

    @Override
    public User extractUserFromPrincipal(Principal principal) {

    	if (principal != null) {
    		if (principal instanceof UsernamePasswordAuthenticationToken) {
    			UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
    			Collection<GrantedAuthority> authorities = token.getAuthorities();
    			
    			// The following code expects that only one Role has been assigned to a test user!
    			GrantedAuthority g = authorities.iterator().hasNext() ? authorities.iterator().next() : null;
    			
    			if (g == null) {
    				return getTestUser();
    			} else if (g.toString().equals(Role.ROLE_USER.toString())) {
    				return getTestUser();
    			} else if (g.toString().equals(Role.ROLE_EDITOR.toString())) {
    				return getEditorUser();
    			} else if (g.toString().equals(Role.ROLE_ADMIN.toString())) {
    				return getAdminUser();
    			} else {
    				LOGGER.error("Unable to determine what role is required for test user");
    			}
    		}
    	}
        return getTestUser();
    }

    public static User getTestUser() {
        User user = new User();
        user.setFullName(DUMMY_USER_FOR_TESTING_ONLY);
        user.setAuthProvider(AuthenticationProvider.GOOGLE);
        user.setFirstLogon(LocalDateTime.now());
        user.setLastLogon(LocalDateTime.now());
        user.setEmail(DUMMY_EMAIL);

        user.setAuthenticationServiceId(THIS_IS_NOT_A_REAL_AUTH_ID);
        user.addRole(User.Role.ROLE_USER);
        return user;
    }

    public static User getEditorUser() {
        User user = getTestUser();
        user.addRole(User.Role.ROLE_EDITOR);
        return user;
    }

    public static User getAdminUser() {
        User user = getEditorUser();
        user.addRole(User.Role.ROLE_ADMIN);
        return user;
    }

    @Override
    public Map<String, Object> getRemoteUserDetails(Principal principal) {
        return new LinkedHashMap<>();
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
