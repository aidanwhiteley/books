package com.aidanwhiteley.books.controller.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI(@Value("${springdoc.version}") String appVersion) {
        Contact contact = new Contact();
        contact.setName("Aidan Whiteley");
        contact.setUrl("https://aidanwhiteley.com");

        return new OpenAPI()
                .components(new Components())
                .info(new Info().title("CloudyBookClub read only API").version(appVersion).contact(contact)
                        .license(new License().name("Apache 2.0").url("https://github.com/aidanwhiteley/books")));
    }
}
