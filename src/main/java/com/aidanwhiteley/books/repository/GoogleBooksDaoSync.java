package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.aidanwhiteley.books.util.ClientInputSanitiserUtils.isValidTitleOrAuthor;
import static com.aidanwhiteley.books.util.ClientInputSanitiserUtils.sanitiseGoogleBookId;
import static com.aidanwhiteley.books.util.LogDetaint.logMessageDetaint;
import static org.springframework.util.StringUtils.hasText;

@Repository
public class GoogleBooksDaoSync {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleBooksDaoSync.class);
    private final GoogleBooksApiConfig googleBooksApiConfig;
    private final RestClient googleBooksRestClient;

    public GoogleBooksDaoSync(GoogleBooksApiConfig googleBooksApiConfig,
                              @Qualifier("googleBooksRestClient") RestClient googleBooksRestClient) {
        this.googleBooksApiConfig = googleBooksApiConfig;
        this.googleBooksRestClient = googleBooksRestClient;
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

        final String searchString = googleBooksApiConfig.getSearchUrl() + "+intitle:" + encodedTitle +
                "+inauthor:" + encodedAuthor + "&" + googleBooksApiConfig.getCountryCode() +
                "&" + googleBooksApiConfig.getMaxResults() +
                (hasText(googleBooksApiConfig.getApiKey()) ? "&key=" + googleBooksApiConfig.getApiKey() : "");

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Google Books API called with API called: {}", searchString);
        }

        BookSearchResult result = googleBooksRestClient.get()
                .uri(searchString)
                .retrieve()
                .body(BookSearchResult.class);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Result of Google Books API call: {}", result);
        }

        return result;
    }

    public Item searchGoogleBooksByGoogleBookId(String id) {

        try {
            String url = googleBooksApiConfig.getGetByIdUrl() +
                    sanitiseGoogleBookId(id) + "/?" +
                    googleBooksApiConfig.getCountryCode() +
                    (hasText(googleBooksApiConfig.getApiKey()) ? "&key=" + googleBooksApiConfig.getApiKey() : "");
            return googleBooksRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(Item.class);
        } catch (HttpStatusCodeException e) {
            String errorpayload = e.getResponseBodyAsString();
            LOGGER.error("Error calling Google Books API: {}", errorpayload, e);
            throw e;
        } catch (RestClientException e) {
            LOGGER.error("Rest client error calling Google Books API: ", e);
            throw e;
        }
    }
}
