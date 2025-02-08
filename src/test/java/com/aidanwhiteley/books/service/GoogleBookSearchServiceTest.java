package com.aidanwhiteley.books.service;

import com.aidanwhiteley.books.domain.googlebooks.Item;
import com.aidanwhiteley.books.service.dtos.GoogleBookSearchResult;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Profile;

import static org.junit.jupiter.api.Assertions.*;

@Profile({"dev-mongo-java-server", "dev-mongo-java-server-no-auth", "dev-mongodb-no-auth", "dev-mongodb", "ci"})
@AutoConfigureWireMock(port = 0, httpsPort = 0)
public class GoogleBookSearchServiceTest extends IntegrationTest {

    @Autowired
    private GoogleBookSearchService googleBookSearchService;

    @Test
    void testGetGoogleBookDataNotInCache() {
        GoogleBookSearchResult result = googleBookSearchService.getGoogleBooks("Head First Design Patterns", "Elisabeth Freeman", 0);
        Item item = result.getItem();
        assertNotNull(item);
        assertFalse(result.isFromCache());
    }

    @Test
    void testGetGoogleBookDataInCache() {
        GoogleBookSearchResult result = googleBookSearchService.getGoogleBooks("Design Patterns", "Gamma", 0);
        Item item = result.getItem();
        assertNotNull(item);
        assertFalse(result.isFromCache());

        GoogleBookSearchResult result2 = googleBookSearchService.getGoogleBooks("Design Patterns", "Gamma", 1);
        Item item2 = result2.getItem();
        assertNotNull(item2);
        assertTrue(result2.isFromCache());
    }
}
