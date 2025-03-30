package com.aidanwhiteley.books.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.aidanwhiteley.books.controller.RestApiExceptionHandler;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.BookRepositoryTest;
import com.aidanwhiteley.books.service.dtos.GoogleBookSearchResult;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@Profile({"dev-mongo-java-server", "dev-mongo-java-server-no-auth", "dev-mongodb-no-auth", "dev-mongodb", "ci"})
@AutoConfigureWireMock(port = 0, httpsPort = 0)
@ActiveProfiles("dev-mongo-java-server")
class GoogleBookSearchServiceTest extends IntegrationTest {

    @Autowired
    private GoogleBookSearchService googleBookSearchService;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void testGetGoogleBookDataNotInCache() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(GoogleBookSearchService.class).setLevel(Level.valueOf("DEBUG"));

        GoogleBookSearchResult result = googleBookSearchService.getGoogleBooks("Head First Design Patterns", "Elisabeth Freeman", 0);
        Item item = result.getItem();
        assertNotNull(item);
        assertFalse(result.isFromCache());

        context.getLogger(GoogleBookSearchService.class).setLevel(Level.valueOf("WARN"));
    }

    @Test
    void testGetGoogleBookDataInCache() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(GoogleBookSearchService.class).setLevel(Level.valueOf("DEBUG"));

        GoogleBookSearchResult result = googleBookSearchService.getGoogleBooks("Design Patterns", "Gamma", 0);
        Item item = result.getItem();
        assertNotNull(item);
        assertFalse(result.isFromCache());

        GoogleBookSearchResult result2 = googleBookSearchService.getGoogleBooks("Design Patterns", "Gamma", 1);
        Item item2 = result2.getItem();
        assertNotNull(item2);
        assertTrue(result2.isFromCache());

        context.getLogger(GoogleBookSearchService.class).setLevel(Level.valueOf("WARN"));
    }

    @Test
    void testUpdateBookWithGoogleDataFromCache() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(GoogleBookSearchService.class).setLevel(Level.valueOf("DEBUG"));

        var result = googleBookSearchService.getGoogleBooks("Design Patterns", "Gamma", 0);
        assertTrue(result.isFromCache());

        Book aBook = bookRepository.insert(BookRepositoryTest.createTestBook());
        Book updatedBook = googleBookSearchService.updateBookWithGoogleBookDetails(aBook,
                "Design Patterns", "Gamma", 0);
        assertNotNull(updatedBook.getGoogleBookDetails());

        Book foundBook = bookRepository.findById(updatedBook.getId()).orElseThrow();
        assertNotNull(foundBook.getGoogleBookDetails());

        context.getLogger(GoogleBookSearchService.class).setLevel(Level.valueOf("WARN"));
    }

}
