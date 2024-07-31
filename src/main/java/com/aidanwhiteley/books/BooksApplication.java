package com.aidanwhiteley.books;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableMongoAuditing
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class BooksApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(BooksApplication.class, args);
    }
}
