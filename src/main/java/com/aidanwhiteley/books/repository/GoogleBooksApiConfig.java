package com.aidanwhiteley.books.repository;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("books.google.books.api")
@Getter
@Setter
public class GoogleBooksApiConfig {

    public static final String UTF_8 = "UTF-8";

    private String searchUrl;

    private String getByIdUrl;

    private String countryCode;

    private int connectTimeout;

    private int readTimeout;
}
