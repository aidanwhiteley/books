package com.aidanwhiteley.books.controller.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    private static final String API_DESCRIPTION = "API documentation for the public read only part of the REST API exposed by the Books application.";

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.aidanwhiteley.books.controller"))
                .paths(PathSelectors.ant("/api/books/**"))
                .build()
                .enableUrlTemplating(true).
                apiInfo(apiInfo());
    }
    
    private ApiInfo apiInfo() {
        return new ApiInfo(
          "Books application public REST API",
                API_DESCRIPTION,
          "V 1.0",
          "https://cloudybookclub.com/#/privacy",
          new Contact("Aidan Whiteley", "https://cloudybookclub.com/", "cloudybookclub@gmail.com"), 
          "Apache 2.0 License", 
          "https://www.apache.org/licenses/LICENSE-2.0", 
          Collections.emptyList());
   }
}
