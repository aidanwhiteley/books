package com.aidanwhiteley.books.controller.config;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.aidanwhiteley.books.controller"))
                .paths(PathSelectors.ant("/api/books/**"))
                .build().
                apiInfo(apiInfo());
    }
    
    private ApiInfo apiInfo() {
        return new ApiInfo(
          "Books public REST API", 
          "API documentation for the read only public REST API exposed by the Books application.", 
          "V 0.2",
          "https://cloudybookclub.com/#/privacy", 
          new Contact("Aidan Whiteley", "https://cloudybookclub.com/", "cloudybookclub@gmail.com"), 
          "Apache 2.0 License", 
          "https://www.apache.org/licenses/LICENSE-2.0", 
          Collections.emptyList());
   }
}
