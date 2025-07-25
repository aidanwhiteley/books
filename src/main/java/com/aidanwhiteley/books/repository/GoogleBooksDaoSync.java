package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static com.aidanwhiteley.books.util.ClientInputSanitiserUtils.isValidTitleOrAuthor;
import static com.aidanwhiteley.books.util.ClientInputSanitiserUtils.sanitiseGoogleBookId;
import static com.aidanwhiteley.books.util.LogDetaint.logMessageDetaint;

@Repository
public class GoogleBooksDaoSync {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleBooksDaoSync.class);
    private final GoogleBooksApiConfig googleBooksApiConfig;
    private RestTemplate googleBooksRestTemplate;

    public GoogleBooksDaoSync(GoogleBooksApiConfig googleBooksApiConfig) {
        this.googleBooksApiConfig = googleBooksApiConfig;
    }

    @PostConstruct
    public void init() {
        // Using a PostConstruct as we need the bean initialised to be able to
        // access the configurable connect and read timeouts
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        this.googleBooksRestTemplate = buildRestTemplate(restTemplateBuilder);
    }

    public BookSearchResult searchGoogleBooksByTitleAndAuthor(String title, String author) {

        if (!isValidTitleOrAuthor(title) || !isValidTitleOrAuthor(author)) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Invalid input for title or author {} {}",
                        logMessageDetaint(title), logMessageDetaint(author));
            }

            throw new IllegalArgumentException("Invalid input for title or author");
        }

        String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
        String encodedAuthor = URLEncoder.encode(author, StandardCharsets.UTF_8);

        googleBooksRestTemplate.getMessageConverters().addFirst(
                new StringHttpMessageConverter(StandardCharsets.UTF_8));

        final String searchString = googleBooksApiConfig.getSearchUrl() + "+intitle:" + encodedTitle +
                "+inauthor:" + encodedAuthor + "&" + googleBooksApiConfig.getCountryCode() +
                "&" + googleBooksApiConfig.getMaxResults();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Google Books API called with API called: {}", searchString);
        }

        BookSearchResult result = googleBooksRestTemplate.getForObject(searchString, BookSearchResult.class);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Result of Google Books API call: {}", result);
        }

        return result;
    }

    public Item searchGoogleBooksByGoogleBookId(String id) {

        googleBooksRestTemplate.getMessageConverters().addFirst(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        try {
            return googleBooksRestTemplate.getForObject(googleBooksApiConfig.getGetByIdUrl() +
                    sanitiseGoogleBookId(id) + "/?" +
                    googleBooksApiConfig.getCountryCode(), Item.class);
        } catch (HttpStatusCodeException e) {
            String errorpayload = e.getResponseBodyAsString();
            LOGGER.error("Error calling Google Books API: {}", errorpayload, e);
            throw e;
        } catch (RestClientException e) {
            LOGGER.error("Rest client error calling Google Books API: ", e);
            throw e;
        }
    }

    private RestTemplate buildRestTemplate(RestTemplateBuilder builder) {

        return builder.connectTimeout(Duration.ofMillis(googleBooksApiConfig.getConnectTimeout())).
                readTimeout(Duration.ofMillis(googleBooksApiConfig.getReadTimeout())).build();
    }
}
