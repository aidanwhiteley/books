package com.aidanwhiteley.books.controller.config;

import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationFilter;
import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSecurityConfigurerAdapter.class);

    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String X_CSRF_TOKEN = "X-CSRF-TOKEN";
    private static final String X_REQUESTED_WITH = "X-Requested-With";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String ORIGIN = "Origin";

    // Fake login page - just sets a 403 / Forbidden response
    public static final String API_LOGIN = "/api/login";

    private final JwtAuthenticationFilter jwtAuththenticationFilter;

    private final JwtAuthenticationService jwtAuthenticationService;

    private final UserService userService;

    @Value("${books.client.enableCORS}")
    private boolean enableCORS;

    @Value("${books.client.allowedCorsOrigin}")
    private String allowedCorsOrigin;

    @Value("${books.client.postLogonUrl}")
    private String postLogonUrl;

    @Autowired
    public WebSecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter, JwtAuthenticationService jwtAuthenticationService,
                                    UserRepository userRepository,
                                    UserService userService) {

        this.jwtAuththenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationService = jwtAuthenticationService;
        this.userService = userService;
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
            // The CSRF cookie is also read and sent by by Angular - hence it being marked as not "httpOnly".
            // The JWT token is stored in a cookie that IS httpOnly.
            http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
        }

        // @formatter:off
        http.
                sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).
                    enableSessionUrlRewriting(false).and().
                antMatcher("/**").authorizeRequests().
                    antMatchers("/api/**", "/login**", "/actuator/info", "/actuator/health").permitAll().
                    antMatchers("/actuator/**").hasRole("ADMIN").
                    anyRequest().authenticated().and().
                addFilterBefore(jwtAuththenticationFilter, UsernamePasswordAuthenticationFilter.class).
                oauth2Login().
                    // The logon page referred to below just returns a 403 - forbidden - as it should only be accessed by APIs.
                    // This doesnt work well for API calls with verbs other than GET as the 302 to the 403 isnt automatically followed
                    // by the client. However, the end effect is still OK as an API call to (say) create a book review when the caller
                    // doesnt have the required permission will result in no update happening and the API getting a 302 response
                    // (meaning the call was blocked).
                    // Having client code look for a 302 to the logon page is just not as intuitive as handling a 403.
                    loginPage(API_LOGIN).
                    authorizationEndpoint().baseUri("/login").
                    authorizationRequestRepository(cookieBasedAuthorizationRequestRepository()).and().
                    successHandler(new Oauth2AuthenticationSuccessHandler()).and().
                headers().referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN);
        // @formatter:on
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

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> cookieBasedAuthorizationRequestRepository() {
    	// Using cookie based repository to avoid data being put into HTTP session
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
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
