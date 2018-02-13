package com.aidanwhiteley.books.controller.jwt;

import com.aidanwhiteley.books.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtils.class);

    private static final String AUTH_PROVIDER = "provider";
    private static final String FULL_NAME = "name";
    private static final String ROLES = "roles";
    private static final String ROLES_DELIMETER = ",";

    @Value("${books.jwt.expiryInMilliSeconds}")
    private int expiryInMilliSeconds;

    @Value("${books.jwt.secretKey}")
    private String secretKey;

    @Value("${books.jwt.issuer}")
    private String issuer;

    public void setExpiryInMilliSeconds(int expiryInMilliSeconds) {
        this.expiryInMilliSeconds = expiryInMilliSeconds;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public User getUserFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();

        String authenticationServiceId = claims.getSubject();
        String issuer = claims.getIssuer();
        String authProvider = (String) claims.get(AUTH_PROVIDER);
        String fullName = (String) claims.get(FULL_NAME);
        String roles = (String) claims.get(ROLES);

        User user = User.builder().
                authenticationServiceId(authenticationServiceId).
                authProvider(User.AuthenticationProvider.valueOf(authProvider)).
                fullName(fullName).
                build();

        String[] rolesArray = roles.split(ROLES_DELIMETER);
        for (String s : rolesArray) {
            user.addRole(User.Role.getRole(Integer.valueOf((String) s)));
        }

        return user;
    }

    public String createTokenForUser(User user) {

        ArrayList<String> roles = new ArrayList<>();
        user.getRoles().forEach(s -> roles.add(String.valueOf(s.getRoleNumber())));

        return Jwts.builder()
                .setSubject(user.getAuthenticationServiceId())
                .setIssuer(issuer)
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .claim(AUTH_PROVIDER, user.getAuthProvider())
                .claim(FULL_NAME, user.getFullName())
                .claim(ROLES, String.join(ROLES_DELIMETER, roles))
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + expiryInMilliSeconds))
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .compact();
    }
}
