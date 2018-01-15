package com.aidanwhiteley.books.filter.config;

import com.aidanwhiteley.books.filter.AuthoritiesFilter;
import com.aidanwhiteley.books.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FilterConfig {

    @Autowired
    private UserRepository userRepository;

    @Bean
    public FilterRegistrationBean filterRegistrationBean() {

        FilterRegistrationBean registration = new FilterRegistrationBean();

        AuthoritiesFilter filter = new AuthoritiesFilter();
        filter.setUserRepository(userRepository);

        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setName("authoritiesFilter");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);

        return registration;
    }
}
