package com.aidanwhiteley.books;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@SpringBootApplication
public class BooksApplication extends WebMvcConfigurerAdapter {

    @Value("${books.client.allowedCorsOrigin}")
    private String allowedCorsOrigin;

    public static void main(String[] args) {
        SpringApplication.run(BooksApplication.class, args);
    }

    @Bean
    @Profile({"dev", "test"})
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {

                System.out.println("Allowed from " + allowedCorsOrigin);
                registry.
                addMapping("/api/**").allowedOrigins(allowedCorsOrigin).allowedMethods("GET");

                registry.
                addMapping("/secure/api/**").allowedOrigins(allowedCorsOrigin).allowedMethods("POST", "PUT", "DELETE");
            }
        };
    }
}
