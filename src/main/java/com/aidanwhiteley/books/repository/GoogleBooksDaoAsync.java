package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

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

    /**
     * The use of the Spring 5 WebClient in this code is more out of a desire to experiment
     * with it rather than for any performance gains.
     * <p>
     * We are calling block() on the Mono so the thread is going to be blocked until
     * the full response from Google Books API has been received. So that will
     * negate any of the usual non blocking benefits of the WebClient usage.
     * <p>
     * We're also continuing to use the blocking Mongo driver in this method.
     * <p>
     * The real benefit to this method is that it is @Async and, therefore, the
     * calling users HTTP call isn't blocked waiting while we go and get / store
     * details from the Google Books API.
     *
     * @param book         Details of the book to update
     * @param googleBookId The Google Books API book id to retrieve.
     */
    @Async("threadPoolExecutor")
    public void updateBookWithGoogleBookDetails(Book book, String googleBookId) {

        LOGGER.debug("Entered updateBookWithGoogleBookDetails");

        try {
            Mono<Item> monoItem = this.webClient.
                    get().
                    uri(booksGoogleBooksApiGetByIdUrl + googleBookId + "/?" + booksGoogleBooksApiCountryCode).
                    retrieve().
                    bodyToMono(Item.class);
            LOGGER.debug("Mono created");

            Item item = monoItem.block(Duration.ofSeconds(booksGoogleBooksApiReadTimeout));
            LOGGER.debug("Block completed");

            bookRepository.addGoogleBookItemToBook(book.getId(), item);
            LOGGER.debug("Google Books details added to Mongo for {}", book.getId());

        } catch (RuntimeException re) {
            LOGGER.error("Error retrieving or storing Google Book details for {}", googleBookId, re);
        }
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
