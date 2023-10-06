package com.aidanwhiteley.books.controller.jwt;

import com.aidanwhiteley.books.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtAuthenticationService jwtService;

    @Value("${books.autoAuthUser}")
    private boolean autoAuthUser;

    @Value("${books.reload.development.data}")
    private boolean reloadDevelopmentData;

    @Autowired
    public JwtAuthenticationFilter(JwtAuthenticationService jwtAuthenticationService) {
        this.jwtService = jwtAuthenticationService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
    throws ServletException, IOException {

        JwtAuthentication auth = jwtService.readAndValidateAuthenticationData(request, response);
        if (auth != null && auth.isAuthenticated()) {
            LOGGER.debug("Setting authentication into SecurityContext");
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else if (auth == null && autoAuthUser && reloadDevelopmentData) {
            // Support for auto authorising a dummy user - but only if the correct
            // config has been set and we are running a profile that drops and
            // recreates all data in the database each time the application starts
            // i.e. we are running a non production system!
            auth = new JwtAuthentication(createDummyUser());
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);
            LOGGER.debug("Setting dummy auth: {}", auth);
        } else {
            LOGGER.debug("Auth status was: {}", auth);
        }
        filterChain.doFilter(request, response);
    }

    private User createDummyUser() {
        User dummyUser = new User();

        // The data below must match the corresponding entry in users.data
        dummyUser.setFirstName("Auto");
        dummyUser.setLastName("Logon");
        dummyUser.setFullName("Auto Logon");
        dummyUser.addRole(User.Role.ROLE_ADMIN);
        dummyUser.setLastLogon(LocalDateTime.MIN);
        dummyUser.setEmail("example@example.com");
        dummyUser.setAuthProvider(User.AuthenticationProvider.LOCAL);
        dummyUser.setAuthenticationServiceId("Dummy12345678");

        return dummyUser;
    }

}
