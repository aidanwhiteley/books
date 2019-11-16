package com.aidanwhiteley.books.controller.jwt;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.util.preprod.MongoJavaServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtAuthenticationService jwtService;

    private final ApplicationContext applicationContext;

    @Value("${books.autoAuthUser}")
    private boolean autoAuthUser;

    @Autowired
    public JwtAuthenticationFilter(JwtAuthenticationService jwtAuthenticationService, ApplicationContext applicationContext) {
        this.jwtService = jwtAuthenticationService;
        this.applicationContext = applicationContext;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
    throws ServletException, IOException {

        JwtAuthentication auth = jwtService.readAndValidateAuthenticationData(request, response);
        if (auth != null && auth.isAuthenticated()) {
            LOGGER.debug("Setting authentication into SecurityContext");
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else if (auth == null && autoAuthUser && isInMemoryMongoDb()) {
            // Support for auto authorising users - but only if the correct
            // config has been set and we are running with an in memory database
            // which will drop and recreate all data every time the application is started.
            auth = new JwtAuthentication(createDummyUser());
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);
            LOGGER.debug("Setting dummy auth: {}", auth);
        } else {
            LOGGER.debug("Auth status was: {}", auth);
        }
        filterChain.doFilter(request, response);
    }

    private boolean isInMemoryMongoDb() {
        boolean inMemoryMongoDb = false;
        try {
            Object mongoConfigObj = applicationContext.getBean("books-mongo-java-server");
            if (mongoConfigObj instanceof MongoJavaServerConfig) {
                MongoJavaServerConfig mongoConfig = (MongoJavaServerConfig) mongoConfigObj;
                if (mongoConfig.getDatabaseName().equalsIgnoreCase(MongoJavaServerConfig.DB_NAME)) {
                    inMemoryMongoDb = true;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to retrieve Mongo client bean from application context", e);
        }

        return inMemoryMongoDb;
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
