package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Repository
public class GoogleBooksDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleBooksDao.class);

    @Value("${books.google.books.api.url}")
    private String booksGoogleBooksApiUrl;

    @Value("${books.google.books.api.connect.timeout}")
    private int booksGoogleBooksApiConnectTimeout;

    @Value("${books.google.books.api.read.timeout}")
    private int booksGoogleBooksApiReadTimeout;

    @Autowired
    private RestTemplate googleBooksRestTemplate;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        builder.setConnectTimeout(booksGoogleBooksApiConnectTimeout);
        builder.setReadTimeout(booksGoogleBooksApiReadTimeout);
        return builder.build();
    }

    public BookSearchResult searchGoogBooksByTitle(String title) {

        String encodedTitle;
        try {
            encodedTitle = URLEncoder.encode(title, "UTF-8");
        } catch (UnsupportedEncodingException usee) {
            LOGGER.error("Unable to encode query string - using as is", usee);
            encodedTitle = title;
        }
            return googleBooksRestTemplate.getForObject(booksGoogleBooksApiUrl + encodedTitle, BookSearchResult.class);
    }
}
