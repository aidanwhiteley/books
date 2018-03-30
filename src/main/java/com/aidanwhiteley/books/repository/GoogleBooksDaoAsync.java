package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Repository
public class GoogleBooksDaoAsync extends GoogleBooksDaoBase {

    public static final String BOOKS_WEB_CLIENT = "Books WebClient";

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleBooksDaoAsync.class);

    private final WebClient webClient;
    private final BookRepository bookRepository;


    @Autowired
    public GoogleBooksDaoAsync(BookRepository bookRepository) {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, BOOKS_WEB_CLIENT)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .build();
        this.bookRepository = bookRepository;
    }

    public void updateBookWithGoogleBookDetails(Book book, String googleBookId) {

        LOGGER.debug("Entered updateBookWithGoogleBookDetails");

        Mono<Item> result = this.webClient.
                get().
                uri(booksGoogleBooksApiGetByIdUrl + googleBookId + "/?" + booksGoogleBooksApiCountryCode).
                retrieve().
                bodyToMono(Item.class);

        result.doOnNext(item -> {
            LOGGER.debug("On next called for item {}", item);
            bookRepository.addGoogleBookItemToBook(book.getId(), item);
            LOGGER.debug("Stored Google book details into repository for book id {}", book.getId());
        });

        LOGGER.debug("Exited updateBookWithGoogleBookDetails");
    }

    public void setBooksGoogleBooksApiSearchUrl(String booksGoogleBooksApiSearchUrl) {
        this.booksGoogleBooksApiSearchUrl = booksGoogleBooksApiSearchUrl;
    }

    public void setBooksGoogleBooksApiGetByIdUrl(String booksGoogleBooksApiGetByIdUrl) {
        this.booksGoogleBooksApiGetByIdUrl = booksGoogleBooksApiGetByIdUrl;
    }

    public void setBooksGoogleBooksApiCountryCode(String booksGoogleBooksApiCountryCode) {
        this.booksGoogleBooksApiCountryCode = booksGoogleBooksApiCountryCode;
    }

    public void setBooksGoogleBooksApiConnectTimeout(int booksGoogleBooksApiConnectTimeout) {
        this.booksGoogleBooksApiConnectTimeout = booksGoogleBooksApiConnectTimeout;
    }

    public void setBooksGoogleBooksApiReadTimeout(int booksGoogleBooksApiReadTimeout) {
        this.booksGoogleBooksApiReadTimeout = booksGoogleBooksApiReadTimeout;
    }

    private ExchangeFilterFunction logRequest() {
        return (clientRequest, next) -> {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Request: {} {}", clientRequest.method(), clientRequest.url());
                clientRequest.headers()
                        .forEach((name, values) -> values.forEach(value -> LOGGER.info("{}={}", name, value)));
            }
            return next.exchange(clientRequest);
        };
    }
}
