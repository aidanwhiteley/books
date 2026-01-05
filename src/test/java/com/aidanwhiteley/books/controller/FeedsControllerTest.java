package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService;
import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.util.BookTestUtils;
import com.aidanwhiteley.books.util.GoodReadsBookExport;
import com.aidanwhiteley.books.util.IntegrationTest;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class FeedsControllerTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${books.feeds.title}")
    private String booksFeedsTitles;

    @Test
    void checkRssFeedsHasEntries() {

        // Find the port the test is running on
        String rootUri = this.testRestTemplate.getRootUri();
        String url = rootUri + "/feeds/rss";

        SyndFeed syndFeed = testRestTemplate.execute(url, HttpMethod.GET, null, response -> {
            SyndFeedInput input = new SyndFeedInput();
            try {
                return input.build(new XmlReader(response.getBody()));
            } catch (FeedException e) {
                fail("Could not parse response", e);
            }
            return null;
        });

        assertEquals(booksFeedsTitles, syndFeed.getTitle());

        assertFalse(syndFeed.getEntries().isEmpty());

        for (SyndEntry entry : syndFeed.getEntries()) {
            assertFalse(entry.getContents().getFirst().getValue().isEmpty());
        }
    }

    @Test
    void checkBooksExportNotLoggedOnHasNoBooks() {
        String rootUri = this.testRestTemplate.getRootUri();
        String url = rootUri + "/feeds/exportbooks";

        ResponseEntity<String> response = testRestTemplate.
                getForEntity(url, String.class);

        // 404 on resource for not logged on user
        assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());

    }

    @Test
    void checkBooksExportWithLoggedOnHasNoBooks() {
        String rootUri = this.testRestTemplate.getRootUri();
        String url = rootUri + "/feeds/exportbooks";

        User user = BookTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Cookie", JwtAuthenticationService.JWT_COOKIE_NAME + "=" + token);

        ResponseEntity<String> response = testRestTemplate.
                exchange(url, HttpMethod.GET, new HttpEntity<>(requestHeaders), String.class);

        // 200 on resource for logged on user
        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        assertTrue(response.getBody().contains(GoodReadsBookExport.goodReadsExportHeaderRow()));
    }
}
