package com.aidanwhiteley.books.controller.jwt;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String SECURE_API_PREFIX = "/secure/api";

	private static final String API_PREFIX = "/api";

	private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtAuthenticationService jwtService;


	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

        JwtAuthentication auth  = jwtService.readAndValidateAuthenticationData(request, response);
        if (auth != null && auth.isAuthenticated()) {
            LOGGER.debug("Setting authentication into SecurityContext");
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
	}
	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request)
            throws ServletException {
		
		if (request.getRequestURI().startsWith(API_PREFIX) || request.getRequestURI().startsWith(SECURE_API_PREFIX)) { 
			LOGGER.debug("Including {}", request.getRequestURI());
			return false;
		} else {
			LOGGER.debug("Excluding {}", request.getRequestURI());
			return true;			
		}
	}

}
