package com.aidanwhiteley.books.filter;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.util.OauthAuthenticationUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * With due thanks to
 * https://stackoverflow.com/questions/46726638/spring-boot-google-oauth2-how-to-define-user-details-service
 */
public class AuthoritiesFilter extends GenericFilterBean {

    private UserRepository userRepository;
    private OauthAuthenticationUtils authUtils;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof OAuth2Authentication) {
            OAuth2Authentication oAuth2Authentication = (OAuth2Authentication) authentication;

            if (oAuth2Authentication != null && oAuth2Authentication.getUserAuthentication().getDetails() != null) {
                SecurityContextHolder.getContext().setAuthentication(processAuthentication(authentication));
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    /**
     * This method is intended to add an existing users roles - known locally to this app -
     * into the authentication object which is based on the oauth data from remore
     * authentication provider.
     *
     * @param authentication
     * @return
     */
    private Authentication processAuthentication(Authentication authentication) {

        OAuth2Authentication oAuth2Authentication = (OAuth2Authentication) authentication;
        Map<String, String> details = (Map<String, String>) oAuth2Authentication.getUserAuthentication().getDetails();

        List<User> users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider(details.get("id"),
                authUtils.getAuthProviderFromAuthAsString(oAuth2Authentication));

        if (users.size() == 1) {
            User user = users.get(0);

            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    oAuth2Authentication.getPrincipal(),
                    oAuth2Authentication.getCredentials(),
                    user.getRoles().stream().map(s -> s.toString()).map(SimpleGrantedAuthority::new).collect(Collectors.toList()));

            oAuth2Authentication = new OAuth2Authentication(oAuth2Authentication.getOAuth2Request(), token);
            oAuth2Authentication.setDetails(details);
        }

        return oAuth2Authentication;
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void setOauthAuthenticationUtils(OauthAuthenticationUtils authUtils) {
        this.authUtils = authUtils;
    }
}
