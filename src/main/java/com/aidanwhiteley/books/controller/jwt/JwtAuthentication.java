package com.aidanwhiteley.books.controller.jwt;

import com.aidanwhiteley.books.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * A class that holds a subset of the User class data based on what is read from a JWT.
 * <p>
 * Contains convenience methods to convert between a this and a (partially populated) User
 * and vice versa.
 */
public class JwtAuthentication implements Authentication {

    private static final long serialVersionUID = 1L;
    private final String fullName;
    private final String authProvider;
    private final String authenticationServiceId;
    private final List<GrantedAuthority> grantedAuthorities = new LinkedList<>();
    private boolean isAuthenticated = false;


    public JwtAuthentication(String fullName, String authProvider, String authenticationServiceId) {
        this.fullName = fullName;
        this.authProvider = authProvider;
        this.authenticationServiceId = authenticationServiceId;
    }

    public JwtAuthentication(User user) {
        this.fullName = user.getFullName();
        this.authProvider = user.getAuthProvider().name();
        this.authenticationServiceId = user.getAuthenticationServiceId();
        user.getRoles().forEach(r -> this.grantedAuthorities.add(new SimpleGrantedAuthority(r.name())));
    }

    public User getUser() {
        User user = User.builder().fullName(this.fullName).
                authProvider(User.AuthenticationProvider.valueOf(this.authProvider)).
                authenticationServiceId(this.authenticationServiceId).
                build();
        for (GrantedAuthority grantedAuthority : this.grantedAuthorities) {
            user.addRole(User.Role.valueOf(grantedAuthority.getAuthority()));
        }

        return user;
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return this.grantedAuthorities;
    }

    @Override
    public Object getCredentials() {
        throw new UnsupportedOperationException("No reason to call get credentials on JWT based authentication!");
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this;
    }

    @Override
    public boolean isAuthenticated() {
        return this.isAuthenticated;
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        this.isAuthenticated = authenticated;
    }

    @Override
    public String getName() {
        return this.fullName;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public String getAuthenticationServiceId() {
        return authenticationServiceId;
    }

    @Override
    public String toString() {
        return "JwtAuthentication{" +
                "grantedAuthorities=" + grantedAuthorities +
                ", isAuthenticated=" + isAuthenticated +
                ", fullName='" + fullName + '\'' +
                ", authProvider='" + authProvider + '\'' +
                ", authenticationServiceId='" + authenticationServiceId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JwtAuthentication that = (JwtAuthentication) o;
        return isAuthenticated == that.isAuthenticated &&
                Objects.equals(grantedAuthorities, that.grantedAuthorities) &&
                Objects.equals(fullName, that.fullName) &&
                Objects.equals(authProvider, that.authProvider) &&
                Objects.equals(authenticationServiceId, that.authenticationServiceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(grantedAuthorities, isAuthenticated, fullName, authProvider, authenticationServiceId);
    }
}
