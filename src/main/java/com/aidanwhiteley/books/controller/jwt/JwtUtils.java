package com.aidanwhiteley.books.controller.jwt;

import java.util.ArrayList;
import java.util.Date;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.aidanwhiteley.books.domain.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.SecretKey;

@Component
public class JwtUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtils.class);

    private static final String AUTH_PROVIDER = "provider";
    private static final String FULL_NAME = "name";
    private static final String ROLES = "roles";
    private static final String ROLES_DELIMETER = ",";

    @Value("${books.jwt.expiryInMilliSeconds}")
    private int expiryInMilliSeconds;

    @Value("${books.jwt.actuatorExpiryInMilliSeconds}")
    private long expiryInMilliSecondsActuator;

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
        byte[] key = Decoders.BASE64.decode(secretKey);
        SecretKey secretKeyCrypto = Keys.hmacShaKeyFor(key);

        Claims claims = Jwts.parser()
                .verifyWith(secretKeyCrypto).build()
                .parseSignedClaims(token)
                .getBody();

        String authenticationServiceId = claims.getSubject();
        String extractedIssuer = claims.getIssuer();
        String authProvider = (String) claims.get(AUTH_PROVIDER);
        String fullName = (String) claims.get(FULL_NAME);
        String roles = (String) claims.get(ROLES);
        
        if (! issuer.equals(extractedIssuer)) {
        	String errMsg = "Expected token issuer of " + issuer + " but found " + extractedIssuer;
        	LOGGER.error(errMsg);
        	throw new IllegalArgumentException(errMsg);
        }

        User user = User.builder().
                authenticationServiceId(authenticationServiceId).
                authProvider(User.AuthenticationProvider.valueOf(authProvider)).
                fullName(fullName).
                build();

        String[] rolesArray = roles.split(ROLES_DELIMETER);
        for (String s : rolesArray) {
            user.addRole(User.Role.getRole(Integer.parseInt(s)));
        }

        return user;
    }

    public String createTokenForUser(User user) {

        ArrayList<String> roles = new ArrayList<>();
        user.getRoles().forEach(s -> roles.add(String.valueOf(s.getRoleNumber())));

        long tokenExpiry = (user.getRoles().size() == 1 && user.getRoles().get(0) == User.Role.ROLE_ACTUATOR)
                ? expiryInMilliSecondsActuator : expiryInMilliSeconds;

        byte[] key = Decoders.BASE64.decode(secretKey);
        SecretKey secretKeyCrypto = Keys.hmacShaKeyFor(key);

        return Jwts.builder()
                .setSubject(user.getAuthenticationServiceId())
                .setIssuer(issuer)
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .claim(AUTH_PROVIDER, user.getAuthProvider())
                .claim(FULL_NAME, user.getFullName())
                .claim(ROLES, String.join(ROLES_DELIMETER, roles))
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + tokenExpiry))
                .signWith(secretKeyCrypto)
                .compact();
    }

    public Date getExpiryFromToken(String token) {
        byte[] key = Decoders.BASE64.decode(secretKey);
        SecretKey secretKeyCrypto = Keys.hmacShaKeyFor(key);

        Claims claims = Jwts.parser()
                .verifyWith(secretKeyCrypto).build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getExpiration();
    }
}
