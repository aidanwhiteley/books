package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.domain.User;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
}
