package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

@Repository
public class GoogleBooksDaoSync extends GoogleBooksDaoBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleBooksDaoSync.class);

    private RestTemplate googleBooksRestTemplate;

    @PostConstruct
    public void init() {
        // Using a PostConstruct as we need the bean initialised to be able to
        // access the configurable connect and read timeouts
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        this.googleBooksRestTemplate = buildRestTemplate(restTemplateBuilder);
    }

    public BookSearchResult searchGoogBooksByTitle(String title) {

        String encodedTitle;
        try {
            encodedTitle = URLEncoder.encode(title, UTF_8);
        } catch (UnsupportedEncodingException usee) {
            LOGGER.error("Unable to encode query string - using as is", usee);
            encodedTitle = title;
        }

        googleBooksRestTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName(UTF_8)));

        return googleBooksRestTemplate.getForObject(booksGoogleBooksApiSearchUrl + encodedTitle + "&" + booksGoogleBooksApiCountryCode,
                BookSearchResult.class);
    }

    public Item searchGoogleBooksByGoogleBookId(String id) {
        googleBooksRestTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName(UTF_8)));
        try {
            return googleBooksRestTemplate.getForObject(booksGoogleBooksApiGetByIdUrl + id + "/?" + booksGoogleBooksApiCountryCode, Item.class);
        } catch (HttpStatusCodeException e) {
            String errorpayload = e.getResponseBodyAsString();
            LOGGER.error("Error calling Google Books API: " + errorpayload, e);
            throw e;
        } catch (RestClientException e) {
            LOGGER.error("Rest client error calling Google Books API: ", e);
            throw e;
        }
    }

    private RestTemplate buildRestTemplate(RestTemplateBuilder builder) {

        return builder.setConnectTimeout(booksGoogleBooksApiConnectTimeout).
                setReadTimeout(booksGoogleBooksApiReadTimeout).build();
    }
}
