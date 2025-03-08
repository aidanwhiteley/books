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
import org.springframework.boot.web.server.Cookie;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.jackson2.CoreJackson2Module;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;
import org.springframework.security.web.servlet.support.csrf.CsrfRequestDataValueProcessor;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;

import java.io.IOException;
import java.util.function.Supplier;

import static com.aidanwhiteley.books.domain.User.Role.ROLE_ACTUATOR;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSecurityConfiguration.class);

    private final JwtAuthenticationFilter jwtAuththenticationFilter;

    private final JwtAuthenticationService jwtAuthenticationService;

    private final UserService userService;

    @Value("${books.client.postLogonUrl}")
    private String postLogonUrl;

    public WebSecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter, JwtAuthenticationService jwtAuthenticationService,
                                    UserService userService) {

        this.jwtAuththenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationService = jwtAuthenticationService;
        this.userService = userService;
    }

    /**
     * The Spring Security set up is driven by the following considerations
     * <ul>
     *     <li>The original, main purpose of this demo application was to build a "microservice" with no HTTP Session state.
     *     This was to promote "scalability" :-) and avoid the need for sticky sessions etc. And just to see
     *     how to do it.</li>
     *     <li>For better or worse, we are using JWT to hold the session logon token. No use of refresh tokens
     *     given that this is a limited demo</li>
     *     <li>Anyone can register on the site via Google or Facebook. This gives no additional privileges compared
     *     to "not logged on" but means that the Spring Security default of "must be authenticated" brings no
     *     benefit in this case - so we don't use it</li>
     *     <li>As of 2025, we are no longer supporting CORS. For SPA client development, stick a "middleware proxy" or something
     *     similar in your SPA config. For production, get a reverse proxy or API gateway. In any case, the
     *     "out of the box" default front end for the microservice is now an HTMX Thymeleaf UI included in the project and,
     *     therefore, on the same protocoal/domain/port</li>
     *     <li>CSRF protection should now always be on and should be working</li>
     *     <li>We use very little "request level" security (a bit for Actuator end points). Instead, we make
     *     heavy use of "method level" security. We apply this at the controller level which may not fully align
     *     with Spring Security best practise, but we can live with this.</li>
     *     <li>While the SPA facing APIs follow consistent URL patterns (/api and /secure/api) that could be
     *     handled with "request level security" (albeit with more granular role checking for /secure/api required),
     *     the URLs for HTMX/Thymeleaf SSR UI are much more "human friendly" and follow no consistent pattern.
     *     The HTMX promoted "locality of behaviour" pattern might also be another argument for preferring
     *     method level security on the controllers</li>
     * </ul>
     *
     * @param http The HTTPSecurity that is being configured
     * @return The Spring SecurityChainFilter
     * @throws Exception See Spring docs
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // We want a new XSRF cookie to be sent on every request/response. See "Opt-out of Deferred CSRF Tokens" at
        // https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
        XorCsrfTokenRequestAttributeHandler requestHandler = new XorCsrfTokenRequestAttributeHandler();
        // Set the name of the attribute the CsrfToken will be populated on to cause the CsrfToken to be loaded on every request.
        requestHandler.setCsrfRequestAttributeName(null);

        final CookieCsrfTokenRepository cookieCsrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        cookieCsrfTokenRepository.setCookieCustomizer((x) -> x.sameSite(Cookie.SameSite.STRICT.attributeValue()));

        CookieClearingLogoutHandler jwtCookie = new CookieClearingLogoutHandler(JwtAuthenticationService.JWT_COOKIE_NAME);
        HeaderWriterLogoutHandler clearSiteData = new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(ClearSiteDataHeaderWriter.Directive.COOKIES));

        http
                .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .enableSessionUrlRewriting(false))
                .csrf((csrf) -> csrf
                        .csrfTokenRepository(cookieCsrfTokenRepository)
                        // With our JWT in a cookie, every time a request with that cookie hits the server an authentication
                        // process takes place and this would trigger a new CSRF token (meaning token checking frequently fails).
                        // See https://github.com/spring-projects/spring-security/issues/5669#issuecomment-855757197
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterAfter(jwtAuththenticationFilter, LogoutFilter.class)
                .oauth2Login(login -> login
                        .authorizationEndpoint(endpoint -> endpoint.baseUri("/login")
                                .authorizationRequestRepository(cookieBasedAuthorizationRequestRepository()))
                        .successHandler(new Oauth2AuthenticationSuccessHandler()))
                .authorizeHttpRequests(authz ->
                        authz
                        // Actuator endpoints are the only ones protected by request level security - everything else by method level security
                        .requestMatchers(EndpointRequest.toAnyEndpoint().excluding(HealthEndpoint.class).excluding(InfoEndpoint.class)).
                        hasRole(ROLE_ACTUATOR.getShortName())
                        // We permitAll here - see the method level JavaDocs for why we do this
                        .anyRequest().permitAll()
                )
                .logout((logout) -> logout.addLogoutHandler(jwtCookie).addLogoutHandler(clearSiteData)
                        .logoutSuccessUrl("/"));
                // We deliberately leave most of the security related header writing to the front end reverse proxy / API gateway
                // to ensure consistency of approach across microservices

        return http.build();
    }

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> cookieBasedAuthorizationRequestRepository() {
        // Using cookie based repository to avoid data being put into HTTP session
        return new HttpCookieOAuth2AuthorizationRequestRepository(getAuthRequestJsonMapper());
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

    // See https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-javascript-spa-configuration
    static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final XorCsrfTokenRequestAttributeHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            /*
             * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection of
             * the CsrfToken when it is rendered in the response body.
             */
            this.xor.setCsrfRequestAttributeName(null);
            this.xor.handle(request, response, csrfToken);
            /*
             * Render the token value to a cookie by causing the deferred token to be loaded.
             */
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            /*
             * If the request contains a request header, use CsrfTokenRequestAttributeHandler
             * to resolve the CsrfToken. This applies when a single-page application includes
             * the header value automatically, which was obtained via a cookie containing the
             * raw CsrfToken.
             *
             * In all other cases (e.g. if the request contains a request parameter), use
             * XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken. This applies
             * when a server-side rendered form includes the _csrf request parameter as a
             * hidden input.
             */
            if (StringUtils.hasText(headerValue)) {
                return this.plain.resolveCsrfTokenValue(request, csrfToken);
            } else {
                return this.xor.resolveCsrfTokenValue(request, csrfToken);
            }
        }
    }

    // For adding CSRF tokens to be added to Thymeleaf templates
    @Bean("csrfRequestDatavalueprocessor")
    public RequestDataValueProcessor requestDataValueProcessor() {
        return new CsrfRequestDataValueProcessor();
    }

}
