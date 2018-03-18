package com.aidanwhiteley.books;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableScheduling
public class BooksApplication implements WebMvcConfigurer  {

    public static void main(String[] args) {
        SpringApplication.run(BooksApplication.class, args);
    }
}
