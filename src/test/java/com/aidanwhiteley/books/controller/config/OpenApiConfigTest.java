package com.aidanwhiteley.books.controller.config;

import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OpenApiConfigTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    void testOpenApiSwaggerUiHtml() {
        ResponseEntity<String> response = testRestTemplate.exchange("/swagger-ui/index.html", HttpMethod.GET,
                null, String.class);

        assertTrue(Objects.requireNonNull(response.getBody()).contains("swagger"));
    }
}
