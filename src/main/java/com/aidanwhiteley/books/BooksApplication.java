package com.aidanwhiteley.books;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@SpringBootApplication
public class BooksApplication extends WebMvcConfigurerAdapter {


    public static void main(String[] args) {
        SpringApplication.run(BooksApplication.class, args);
    }


}
