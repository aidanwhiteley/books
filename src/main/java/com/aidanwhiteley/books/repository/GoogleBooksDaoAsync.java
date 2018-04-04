package com.aidanwhiteley.books.repository;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.googlebooks.Item;

import reactor.core.publisher.Mono;

@Repository
public class GoogleBooksDaoAsync extends GoogleBooksDaoBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleBooksDaoAsync.class);

    private static final String BOOKS_WEB_CLIENT = "Books WebClient";

    private final WebClient webClient;
    private final BookRepository bookRepository;

    @Autowired
    public GoogleBooksDaoAsync(BookRepository bookRepository) {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, BOOKS_WEB_CLIENT)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponseStatus())
                .build();
        this.bookRepository = bookRepository;
    }

    @Async("threadPoolExecutor")
    public void updateBookWithGoogleBookDetails(Book book, String googleBookId) {

        LOGGER.debug("Entered updateBookWithGoogleBookDetails");

        CountDownLatch countDownLatch = new CountDownLatch(1);

        this.webClient.
                get().
                uri(booksGoogleBooksApiGetByIdUrl + googleBookId + "/?" + booksGoogleBooksApiCountryCode).
                retrieve().
                bodyToMono(Item.class).
                doAfterSuccessOrError((t1, t2) -> countDownLatch.countDown()).
                timeout(Duration.ofSeconds(5)).
                doOnNext(item -> bookRepository.addGoogleBookItemToBook(book.getId(), item)).
                subscribe();
        LOGGER.debug("WebClient call to Google Books API initiated");
        
        try {
        	LOGGER.debug("Awaiting completion of Google Books API API call");
            countDownLatch.await();
            LOGGER.debug("Completion of Google Books API API call - await finished");
        } catch (InterruptedException ie) {
            LOGGER.error("InterruptedException while waiting for countDownLatch", ie);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unexpected InterruptedException while waiting for countDownLatch - see logs");
        }

        LOGGER.debug("Exited updateBookWithGoogleBookDetails");
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

    private ExchangeFilterFunction logResponseStatus() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            LOGGER.info("Response Status {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}
