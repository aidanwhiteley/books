package com.aidanwhiteley.books.controller.config;

import static com.aidanwhiteley.books.domain.User.Role.ROLE_ADMIN;
import static com.aidanwhiteley.books.domain.User.Role.ROLE_EDITOR;
import static com.aidanwhiteley.books.domain.User.Role.ROLE_USER;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@Profile("integration")
@EnableGlobalMethodSecurity(prePostEnabled = true)
/**
 * The tests run with simple basic auth rather than oauth. All we are doing is
 * checking that the correct method level authorisations are working.
 *
 * Testing of logon against the remote oauth authentication providers works
 * correctly is done through manual testing.
 */
public class BasicAuthInsteadOfOauthWebAccess extends WebSecurityConfigurerAdapter {

    public static final String A_USER = "user";
    public static final String AN_EDITOR = "editor";
    public static final String AN_ADMIN = "admin";
    public static final String PASSWORD = "notaRealPassword";

    @Override
    public void configure(HttpSecurity web) throws Exception {

        web.cors()
                .and()
                .csrf().disable()
                .antMatcher("/**")
                .authorizeRequests()
                .antMatchers("/api/**", "/login**")
                .permitAll()
                .anyRequest()
                .authenticated()
                .and().httpBasic();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {

        auth.inMemoryAuthentication()
                .withUser(AN_EDITOR)
                .password(PASSWORD)
                .roles(ROLE_EDITOR.getShortName());
        auth.inMemoryAuthentication()
                .withUser(AN_ADMIN)
                .password(PASSWORD)
                .roles(ROLE_ADMIN.getShortName());
        auth.inMemoryAuthentication()
                .withUser(A_USER)
                .password(PASSWORD)
                .roles(ROLE_USER.getShortName());
    }

//    @Override
//    public void configure(WebSecurity web) throws Exception {
//        web.debug(true);
//    }

}
