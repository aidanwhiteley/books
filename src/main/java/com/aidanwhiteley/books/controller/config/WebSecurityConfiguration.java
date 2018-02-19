package com.aidanwhiteley.books.controller.config;

import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationFilter;
import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.domain.User.AuthenticationProvider;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.filter.CompositeFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Supports oauth2 based social logons and JWT based authentication and authorisation.
 * <p>
 * Initially based on the details at
 * https://spring.io/guides/tutorials/spring-boot-oauth2/
 * but all additions and bugs are my own!
 */
@EnableOAuth2Client
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSecurityConfigurerAdapter.class);

    private final JwtAuthenticationFilter jwtAuththenticationFilter;

    private final JwtAuthenticationService jwtAuthenticationService;

    private final OAuth2ClientContext oauth2ClientContext;

    private final UserRepository userRepository;

    private final UserService userService;

    @Value("${books.client.enableCORS}")
    private boolean enableCORS;

    @Value("${books.client.allowedCorsOrigin}")
    private String allowedCorsOrigin;

    @Value("${books.client.postLogonUrl}")
    private String postLogonUrl;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public WebSecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter, JwtAuthenticationService jwtAuthenticationService,
                                    OAuth2ClientContext oAuth2ClientContext, UserRepository userRepository,
                                    UserService userService) {
        // Note - IntelliJ claims that there are multiple possible providers for the OAuth2ClientContext but I
        //        cant find them! The code works - so warning suppressed.

        this.jwtAuththenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationService = jwtAuthenticationService;
        this.oauth2ClientContext = oAuth2ClientContext;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Bean
    @ConfigurationProperties("google")
    public ClientResources google() {
        return new ClientResources();
    }

    @Bean
    @ConfigurationProperties("facebook")
    public ClientResources facebook() {
        return new ClientResources();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        // Is CORS to be enabled? If yes, the allowedCorsOrigin config
        // property should also be set.
        // Normally only expected to be used in dev when there is no "front
        // proxy" of some sort
        if (enableCORS) {
            http.cors();
        }

        // Getting required server side config for enabling Angular to send X-CSRF-TOKEN request header across
        // CORS domains has currently defeated me.
        // Client side this wouldnt work out of the box with Angular either but the following library would
        // probably help if I could get the server side config right.
        // https://github.com/pasupulaphani/angular-csrf-cross-domain
        //
        // So if using CORS, there's no XSRF protection!
        if (enableCORS) {
            http.csrf().disable();

            LOGGER.warn("****************************************************************************");
            LOGGER.warn("*** WARNING!                                                             ***");
            LOGGER.warn("*** You are running with CORS enabled. This is only supported for        ***");
            LOGGER.warn("*** development.                                                         ***");
            LOGGER.warn("*** There is no cross site request forgery prevention in place when      ***");
            LOGGER.warn("*** running with CORS enabled. Change the settings in the .yml files     ***");
            LOGGER.warn("*** if you are not developing locally.                                   ***");
            LOGGER.warn("****************************************************************************");
        } else {
            http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
        }

        // @formatter:off
        http.
                // We dont currently use the Spring Security logout functionality as I _think_ that it is ahead of the
                // CORS config filter meaning that a cross domain request to logoff will fail.
                // Instead there is a custom method in the UserController class.

                //logout().deleteCookies(JwtAuthenticationService.JWT_COOKIE_NAME, JwtAuthenticationService.JSESSIONID_COOKIE_NAME)
                //   .logoutSuccessUrl(postLogonUrl).and().
                sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).
                enableSessionUrlRewriting(false).and().
                antMatcher("/**").authorizeRequests().
                antMatchers("/api/**", "/login**").permitAll().
                anyRequest().authenticated().and().
                addFilterBefore(jwtAuththenticationFilter, UsernamePasswordAuthenticationFilter.class).
                addFilterBefore(oauth2SsoFilter(), BasicAuthenticationFilter.class);
        // @formatter:on
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                if (enableCORS) {
                    registry.addMapping("/api/**").allowedOrigins(allowedCorsOrigin).allowedMethods("GET").allowedHeaders("origin", "content-type", "X-CSRF-TOKEN");
                    registry.addMapping("/secure/api/**").allowedOrigins(allowedCorsOrigin).allowedMethods("GET",
                            "POST", "PUT", "DELETE", "PATCH", "OPTIONS").allowedHeaders("Origin", "Content-Type", "X-CSRF-TOKEN", "X-Requested-With");
                }
            }
        };
    }

    private Filter oauth2SsoFilter() {
        CompositeFilter filter = new CompositeFilter();
        List<Filter> filters = new ArrayList<>();
        filters.add(oauth2SsoFilter(facebook(), "/login/facebook", AuthenticationProvider.FACEBOOK));
        filters.add(oauth2SsoFilter(google(), "/login/google", AuthenticationProvider.GOOGLE));
        filter.setFilters(filters);
        return filter;
    }


    private Filter oauth2SsoFilter(ClientResources client, String path, AuthenticationProvider provider) {

        OAuth2ClientAuthenticationProcessingFilter filter = new OAuth2ClientAuthenticationProcessingFilter(path);
        filter.setAllowSessionCreation(false);
        filter.setAuthenticationSuccessHandler(new SimpleUrlAuthenticationSuccessHandler() {
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {

                OAuth2Authentication auth2 = (OAuth2Authentication) authentication;
                User user = userService.createOrUpdateUser(auth2);

                jwtAuthenticationService.setAuthenticationData(request, response, user);

                this.setDefaultTargetUrl(postLogonUrl);
                super.onAuthenticationSuccess(request, response, authentication);
            }

        });

        OAuth2RestTemplate template = new OAuth2RestTemplate(client.getClient(), oauth2ClientContext);
        filter.setRestTemplate(template);

        UserInfoTokenServices tokenServices = new UserInfoTokenServices(client.getResource().getUserInfoUri(),
                client.getClient().getClientId());
        tokenServices.setRestTemplate(template);
        tokenServices.setAuthoritiesExtractor(new SocialAuthoritiesExtractor(provider));
        filter.setTokenServices(tokenServices);

        return filter;
    }

    @Bean
    public FilterRegistrationBean oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(filter);
        registration.setOrder(-100);
        return registration;
    }

    class ClientResources {

        @NestedConfigurationProperty
        private final AuthorizationCodeResourceDetails client = new AuthorizationCodeResourceDetails();

        @NestedConfigurationProperty
        private final ResourceServerProperties resource = new ResourceServerProperties();

        public AuthorizationCodeResourceDetails getClient() {
            return client;
        }

        public ResourceServerProperties getResource() {
            return resource;
        }
    }

    class SocialAuthoritiesExtractor implements AuthoritiesExtractor {
        private final String authProvider;

        public SocialAuthoritiesExtractor(User.AuthenticationProvider authProvider) {
            super();
            this.authProvider = authProvider.toString();
        }

        @Override
        public List<GrantedAuthority> extractAuthorities(Map<String, Object> map) {

            List<User> users = userRepository.findAllByAuthenticationServiceIdAndAuthProvider((String) map.get("id"),
                    authProvider);

            if (users.size() == 1) {
                String csvRoles = users.get(0).getRoles().stream().map(s -> s.toString())
                        .collect(Collectors.joining(","));
                return AuthorityUtils.commaSeparatedStringToAuthorityList(csvRoles);
            } else {
                return AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER");
            }
        }
    }

}
