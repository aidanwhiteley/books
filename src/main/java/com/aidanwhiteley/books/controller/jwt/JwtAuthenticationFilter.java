package com.aidanwhiteley.books.controller.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtAuthenticationService jwtService;

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
        } else {
            LOGGER.debug("Auth status was: {}", auth);
        }
        filterChain.doFilter(request, response);
    }

}
