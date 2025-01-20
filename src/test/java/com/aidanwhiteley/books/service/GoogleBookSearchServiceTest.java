package com.aidanwhiteley.books.service;

import com.aidanwhiteley.books.domain.googlebooks.Item;
import com.aidanwhiteley.books.service.dtos.GoogleBookSearchResult;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Profile;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Profile({"dev-mongo-java-server", "dev-mongo-java-server-no-auth", "dev-mongodb-no-auth", "dev-mongodb", "ci"})
@AutoConfigureWireMock(port=0, httpsPort = 0)
public class GoogleBookSearchServiceTest extends IntegrationTest {

    @Autowired
    private GoogleBookSearchService googleBookSearchService;

    @Test
    void testGetGoogleBookDataNotInCache() {
        GoogleBookSearchResult result = googleBookSearchService.getGoogleBooks("Design Patterns", "Gamma", 0);
        Item item = result.getItem();
        System.out.println("Saw: " + item);
        assertNotNull(item);
    }
}
