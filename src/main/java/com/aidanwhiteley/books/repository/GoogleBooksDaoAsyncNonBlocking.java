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

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Repository
public class GoogleBooksDaoAsyncNonBlocking extends GoogleBooksDaoBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleBooksDaoAsyncNonBlocking.class);

    private static final String BOOKS_WEB_CLIENT = "Books WebClient";

    private final WebClient webClient;
    private final BookRepository bookRepository;

    @Autowired
    public GoogleBooksDaoAsyncNonBlocking(BookRepository bookRepository) {
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

        // TODO - remove the usage of CountDownLatch
        CountDownLatch countDownLatch = new CountDownLatch(1);

        // Using a WebClient to try and run HTTP call to "Google" without blocking the thread
        // it is running in while waiting for a response from Google.
        // All of which is _utterly_ pointless while the code uses the blocking CountDownLatch construct :-)
        // Lets call it a work in progress!
        Disposable disposable = this.webClient.
                get().
                uri(booksGoogleBooksApiGetByIdUrl + googleBookId + "/?" + booksGoogleBooksApiCountryCode).
                retrieve().
                bodyToMono(Item.class).
                doAfterSuccessOrError((t1, t2) -> countDownLatch.countDown()).
                timeout(Duration.ofSeconds(5)).
                doOnNext(item -> {
                    bookRepository.addGoogleBookItemToBook(book.getId(), item);
                    LOGGER.debug("Google Books details added for {}", book.getId());
                }).
                subscribe();
        LOGGER.debug("WebClient call to Google Books API initiated");
        
        try {
        	LOGGER.debug("Awaiting completion of Google Books API API call");
            countDownLatch.await();
            LOGGER.debug("Completion of Google Books API call - CountDownLatch await finished");
        } catch (InterruptedException ie) {
            LOGGER.error("InterruptedException while waiting for countDownLatch", ie);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unexpected InterruptedException while waiting for countDownLatch - see logs");
        } finally {
            disposable.dispose();
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
