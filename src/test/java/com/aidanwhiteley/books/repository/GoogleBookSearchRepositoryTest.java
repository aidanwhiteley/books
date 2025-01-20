package com.aidanwhiteley.books.repository;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import com.aidanwhiteley.books.repository.dtos.GoogleBookSearch;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Profile({"dev-mongo-java-server", "dev-mongo-java-server-no-auth", "dev-mongodb-no-auth", "dev-mongodb", "ci"})
@AutoConfigureWireMock(port=0, httpsPort = 0)
class GoogleBookSearchRepositoryTest extends IntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleBookSearchRepositoryTest.class);
    private static final int NUMBER_OF_BOOKS_IN_SEARCH_RESULTS = 30;

    @Autowired
    private GoogleBooksDaoSync theDao;

    @Autowired
    private GoogleBookSearchRepository searchRepository;

    @Test
    void findByTitleAndAuthor() {
        BookSearchResult result = theDao.searchGoogBooksByTitleAndAuthor("Design Patterns", "Gamma");
        assertNotNull(result);

        GoogleBookSearch gbSearch =
                new GoogleBookSearch("Design Patterns", "Gamma", result, LocalDateTime.now().plusMinutes(1));
        searchRepository.insert(gbSearch);
        assertEquals(1, searchRepository.findAllByTitleAndAuthor("Design Patterns", "Gamma").size());
    }


}