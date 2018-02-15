package com.aidanwhiteley.books.controller.jwt;

import com.aidanwhiteley.books.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class JwtAuthentication implements Authentication {

	private static final long serialVersionUID = 1L;
	
	private List<GrantedAuthority> grantedAuthorities = new LinkedList<>();
    private boolean isAuthenticated = false;
    private String fullName;
    private String authProvider;
    private String authenticationServiceId;


    public JwtAuthentication(String fullName, String authProvider, String authenticationServiceId) {
        this.fullName = fullName;
        this.authProvider = authProvider;
        this.authenticationServiceId = authenticationServiceId;
    }

    public JwtAuthentication(String fullName, String authProvider, String authenticationServiceId, boolean isAuthenticated,
                             List<GrantedAuthority> grantedAuthorities) {
        this.fullName = fullName;
        this.authProvider = authProvider;
        this.authenticationServiceId = authenticationServiceId;
        this.isAuthenticated = isAuthenticated;
        this.grantedAuthorities = grantedAuthorities;
    }

    public JwtAuthentication(User user) {
        this.fullName = user.getFullName();
        this.authProvider = user.getAuthProvider().name();
        this.authenticationServiceId = user.getAuthenticationServiceId();
        user.getRoles().forEach(r -> this.grantedAuthorities.add(new SimpleGrantedAuthority(r.name())));
    }

    public User getUser() {
        User user =  User.builder().fullName(this.fullName).
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
        // TODO - work out what to return here
        return null;
    }

    @Override
    public Object getPrincipal() {
        // TODO - work out what to return here
        return this;
    }

    @Override
    public boolean isAuthenticated() {
        return this.isAuthenticated;
    }

    @Override
    public String getName() {
        return this.fullName;
    }

    @Override
    public void setAuthenticated(boolean authenticated) throws IllegalArgumentException {
        this.isAuthenticated = authenticated;
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
