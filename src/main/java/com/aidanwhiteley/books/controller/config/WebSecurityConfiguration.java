package com.aidanwhiteley.books.controller.config;

import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationFilter;
import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.service.UserService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.jackson2.CoreJackson2Module;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;

import static com.aidanwhiteley.books.domain.User.Role.ROLE_ACTUATOR;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableMethodSecurity()
public class WebSecurityConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSecurityConfiguration.class);

    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String X_CSRF_TOKEN = "X-CSRF-TOKEN";
    private static final String X_REQUESTED_WITH = "X-Requested-With";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String ORIGIN = "Origin";

    private static final RequestMatcher PUBLIC_URLS = new OrRequestMatcher(
            new AntPathRequestMatcher("/api/**"),
            new AntPathRequestMatcher("/login**"),
            new AntPathRequestMatcher("/feeds/**"),
            new AntPathRequestMatcher("/favicon.ico"),
            // And some paths just for playing with SWAGGER UI within the same app
            new AntPathRequestMatcher("/swagger-resources/**"),
            new AntPathRequestMatcher("/swagger-ui.html"),
            new AntPathRequestMatcher("/v2/api-docs"),
            new AntPathRequestMatcher("/webjars/**")
    );
    private static final RequestMatcher PROTECTED_URLS = new NegatedRequestMatcher(PUBLIC_URLS);

    private final JwtAuthenticationFilter jwtAuththenticationFilter;

    private final JwtAuthenticationService jwtAuthenticationService;

    private final UserService userService;

    @Value("${books.client.enableCORS}")
    private boolean enableCORS;

    @Value("${books.client.allowedCorsOrigin}")
    private String allowedCorsOrigin;

    @Value("${books.client.postLogonUrl}")
    private String postLogonUrl;

    public WebSecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter, JwtAuthenticationService jwtAuthenticationService,
                                    UserService userService) {

        this.jwtAuththenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationService = jwtAuthenticationService;
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // Is CORS to be enabled? If yes, the allowedCorsOrigin config
        // property should also be set.
        // Normally only expected to be used in dev when there is no "front
        // proxy" of some sort
        if (enableCORS) {
        }

        // Getting required server side config for enabling Angular to send X-CSRF-TOKEN request header across
        // CORS domains has currently defeated me.
        // Client side this wouldnt work out of the box with Angular either but the following library would
        // probably help if I could get the server side config right.
        // https://github.com/pasupulaphani/angular-csrf-cross-domain
        //
        // So if using CORS, there's no XSRF protection!
        if (enableCORS) {
            http.csrf(csrf -> csrf.disable());      // lgtm [java/spring-disabled-csrf-protection]
            LOGGER.warn("");
            LOGGER.warn("**********************************************************************");
            LOGGER.warn("*** WARNING!                                                       ***");
            LOGGER.warn("*** You are running with CORS enabled. This is only supported for  ***");
            LOGGER.warn("*** development.                                                   ***");
            LOGGER.warn("*** There is no cross site request forgery prevention when         ***");
            LOGGER.warn("*** running with CORS enabled. Change settings in the .yml files   ***");
            LOGGER.warn("*** if you are not developing locally.                             ***");
            LOGGER.warn("**********************************************************************");
            LOGGER.warn("");
        } else {
            // See https://docs.spring.io/spring-security/reference/5.8/migration/servlet/exploits.html#_defer_loading_csrftoken
            // for why the following becomes necessary with Spring Security >= 5.8 and our use of AngularJS
            CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
            requestHandler.setCsrfRequestAttributeName(null);

            // The CSRF cookie is also read and sent by Angular - hence it being marked as not "httpOnly".
            // The JWT token is stored in a cookie that IS httpOnly.
            http.csrf(csrf -> csrf.
                    csrfTokenRequestHandler(requestHandler).
                    csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
        }

        http
                .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS).enableSessionUrlRewriting(false))
                .authorizeHttpRequests(authz ->
                        authz
                                // Make sure Actuator endpoints are protected
                                .requestMatchers(EndpointRequest.toAnyEndpoint().excluding(HealthEndpoint.class).excluding(InfoEndpoint.class)).
                                hasRole(ROLE_ACTUATOR.getShortName())
                                // We permitAll here (getting us back to the Spring Boot 2 default) as we have method level security
                                // applied rather than request level
                                .anyRequest().permitAll()
                )
                .exceptionHandling(handling -> handling
                        .defaultAuthenticationEntryPointFor(forbiddenEntryPoint(), PROTECTED_URLS))
                .addFilterBefore(jwtAuththenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(login -> login
                        .authorizationEndpoint(endpoint -> endpoint.baseUri("/login")
                                .authorizationRequestRepository(cookieBasedAuthorizationRequestRepository()))
                        .successHandler(new Oauth2AuthenticationSuccessHandler()))
                .formLogin(login -> login.disable())
                .httpBasic(basic -> basic.disable())
                .headers(headers -> headers.referrerPolicy(policy -> policy.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));

        return http.build();

    }

    @Bean
    protected AuthenticationEntryPoint forbiddenEntryPoint() {
        return new HttpStatusEntryPoint(FORBIDDEN);
    }

    /**
     * This is where we trigger the work to store local details for the user after they have successfully
     * authenticated with the OAuth2 authentication provider.
     */
    class Oauth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                            Authentication authentication) throws IOException, ServletException {

            OAuth2AuthenticationToken auth2 = (OAuth2AuthenticationToken) authentication;
            User user = userService.createOrUpdateUser(auth2);
            jwtAuthenticationService.setAuthenticationData(response, user);
            super.setDefaultTargetUrl(postLogonUrl);
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }


    public static ObjectMapper getAuthRequestJsonMapper() {
        var mapper = new Jackson2ObjectMapperBuilder().autoDetectFields(true)
                .autoDetectGettersSetters(true)
                .modules(new OAuth2ClientJackson2Module())
                .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
                .build();
        // See https://github.com/spring-projects/spring-security/issues/4370
        mapper.registerModule(new CoreJackson2Module());

        return mapper;
    }

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> cookieBasedAuthorizationRequestRepository() {
        // Using cookie based repository to avoid data being put into HTTP session
        return new HttpCookieOAuth2AuthorizationRequestRepository(getAuthRequestJsonMapper());
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {

        //noinspection NullableProblems
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                if (enableCORS) {
                    registry.addMapping("/api/**").allowedOrigins(allowedCorsOrigin).
                            allowedMethods("GET").allowedHeaders(ORIGIN, CONTENT_TYPE, X_CSRF_TOKEN, ACCESS_CONTROL_ALLOW_CREDENTIALS).
                            allowCredentials(true);
                    registry.addMapping("/secure/api/**").allowedOrigins(allowedCorsOrigin).
                            allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS").
                            allowedHeaders(ORIGIN, CONTENT_TYPE, X_CSRF_TOKEN, X_REQUESTED_WITH, ACCESS_CONTROL_ALLOW_CREDENTIALS).
                            allowCredentials(true);
                    registry.addMapping("/login/**").allowedOrigins(allowedCorsOrigin).
                            allowedMethods("GET", "POST", "OPTIONS").
                            allowedHeaders(ORIGIN, CONTENT_TYPE, X_CSRF_TOKEN, X_REQUESTED_WITH, ACCESS_CONTROL_ALLOW_CREDENTIALS).
                            allowCredentials(true);
                }
            }
        };
    }
}
