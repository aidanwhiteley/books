package com.aidanwhiteley.books.controller.config;

import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

class SwaggerConfigTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private Docket docket;

    @Test
    void testSwaggerUiHtml() {
        ResponseEntity<String> response = testRestTemplate.exchange("/swagger-ui.html", HttpMethod.GET,
                null, String.class);

        //noinspection ConstantConditions
        assertTrue(response.getBody().contains("swagger"));
    }

    @Test
    void testSwggaerDocumentationType() {
        DocumentationType docType = docket.getDocumentationType();
        assertEquals(DocumentationType.SWAGGER_2, docType);
    }
}
