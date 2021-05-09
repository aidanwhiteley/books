package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.util.IntegrationTest;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedsControllerTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Value("${books.feeds.title}")
    private String booksFeedsTitles;

    @Test
    void checkRssFeedsHasEntries() throws Exception {

        // Find the port the test is running on
        String rootUri = this.testRestTemplate.getRootUri();
        String url = rootUri + "/feeds/rss";

        XmlReader reader = new XmlReader(new URL(url));
        SyndFeed feed = new SyndFeedInput().build(reader);
        assertEquals(booksFeedsTitles, feed.getTitle());

        assertTrue(feed.getEntries().size() > 0);

        for (SyndEntry entry : feed.getEntries()) {
            assertFalse(entry.getContents().get(0).getValue().isEmpty());
        }
    }
}
