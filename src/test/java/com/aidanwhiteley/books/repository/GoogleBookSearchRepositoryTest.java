package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.repository.dtos.GoogleBookSearch;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Profile({"dev-mongo-java-server", "dev-mongo-java-server-no-auth", "dev-mongodb-no-auth", "dev-mongodb", "ci"})
@EnableWireMock({
        @ConfigureWireMock(
                httpsPort = 0,
                port = 0)
})
@ActiveProfiles("dev-mongo-java-server")
class GoogleBookSearchRepositoryTest extends IntegrationTest {

    @Autowired
    private GoogleBooksDaoSync theDao;

    @Autowired
    private GoogleBookSearchRepository searchRepository;

    @Test
    void findByTitleAndAuthor() {
        BookSearchResult result = theDao.searchGoogleBooksByTitleAndAuthor("Design Patterns", "Gamma");
        assertNotNull(result);

        // Now using fake title, author when inserting into cache to avoid conflict with other tests (no rollback after test!).
        GoogleBookSearch gbSearch =
                new GoogleBookSearch("Dummy title", "Dummy author", result, LocalDateTime.now().plusMinutes(1));
        searchRepository.insert(gbSearch);
        assertEquals(1, searchRepository.findAllByTitleAndAuthor("Dummy title", "Dummy author").size());
    }


}