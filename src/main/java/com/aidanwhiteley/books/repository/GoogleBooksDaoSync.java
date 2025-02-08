package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import com.aidanwhiteley.books.domain.googlebooks.VolumeInfo;
import com.aidanwhiteley.books.util.HtmlSanitiserUtils;
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

@Repository
public class GoogleBooksDaoSync {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleBooksDaoSync.class);
    private final GoogleBooksApiConfig googleBooksApiConfig;
    private RestTemplate googleBooksRestTemplate;
    private final BookRepository bookRepository;

    public GoogleBooksDaoSync(GoogleBooksApiConfig googleBooksApiConfig, BookRepository bookRepository) {
        this.googleBooksApiConfig = googleBooksApiConfig;
        this.bookRepository = bookRepository;
    }

    @PostConstruct
    public void init() {
        // Using a PostConstruct as we need the bean initialised to be able to
        // access the configurable connect and read timeouts
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        this.googleBooksRestTemplate = buildRestTemplate(restTemplateBuilder);
    }

    public BookSearchResult searchGoogBooksByTitleAndAuthor(String title, String author) {

        String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
        String encodedAuthor = URLEncoder.encode(author, StandardCharsets.UTF_8);

        googleBooksRestTemplate.getMessageConverters().add(0,
                new StringHttpMessageConverter(StandardCharsets.UTF_8));

        final String searchString = googleBooksApiConfig.getSearchUrl() + "+intitle:" + encodedTitle +
                "+inauthor:" + encodedAuthor + "&" + googleBooksApiConfig.getCountryCode() +
                "&" + googleBooksApiConfig.getMaxResults();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Google Books API called with API called: {}", searchString);
        }

        BookSearchResult result =  googleBooksRestTemplate.getForObject(searchString, BookSearchResult.class);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Result of Google Books API call: {}", result);
        }

        return result;
    }

    public Item searchGoogleBooksByGoogleBookId(String id) {

        googleBooksRestTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        try {
            return googleBooksRestTemplate.getForObject(googleBooksApiConfig.getGetByIdUrl() + id + "/?" +
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

    public void updateBookWithGoogleBookDetails(Book book, String googleBookId) {
        Item item = searchGoogleBooksByGoogleBookId(googleBookId);

        // Google Books API data _should_ be safe from CSRF attacks but lets make sure before storing the
        // description text in the database!
        VolumeInfo vlInfo = item.getVolumeInfo();
        if (vlInfo != null && vlInfo.getDescription() != null) {
            vlInfo.setDescription(HtmlSanitiserUtils.allowBasicTextFormattingOnly(vlInfo.getDescription()));
            item.setVolumeInfo(vlInfo);
        }

        bookRepository.addGoogleBookItemToBook(book.getId(), item);
        LOGGER.debug("Google Books details added to Mongo for {}", book.getId());
    }


    private RestTemplate buildRestTemplate(RestTemplateBuilder builder) {

        return builder.setConnectTimeout(Duration.ofMillis(googleBooksApiConfig.getConnectTimeout())).
                setReadTimeout(Duration.ofMillis(googleBooksApiConfig.getReadTimeout())).build();
    }
}
