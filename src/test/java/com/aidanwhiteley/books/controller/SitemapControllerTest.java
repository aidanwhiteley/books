package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class SitemapControllerTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    void sitemapXmlEndpointReturnsXmlWithExpectedUrls() throws Exception {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/sitemap.xml", String.class);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getHeaders().getContentType());
        assertEquals("application", response.getHeaders().getContentType().getType());
        assertEquals("xml", response.getHeaders().getContentType().getSubtype());
        assertNotNull(response.getBody());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(response.getBody())));

        assertEquals("urlset", document.getDocumentElement().getLocalName());

        NodeList locNodes = document.getElementsByTagNameNS("*", "loc");
        List<String> locations = new ArrayList<>();
        for (int i = 0; i < locNodes.getLength(); i++) {
            locations.add(locNodes.item(i).getTextContent());
        }

        assertTrue(locations.contains("https://cloudybookclub.com/"));
        assertTrue(locations.contains("https://cloudybookclub.com/recent"));
        assertTrue(locations.contains("https://cloudybookclub.com/find"));
        assertTrue(locations.contains("https://cloudybookclub.com/privacy"));
        assertTrue(locations.stream().anyMatch(location -> location.contains("/bookreview?bookId=")));
    }
}

