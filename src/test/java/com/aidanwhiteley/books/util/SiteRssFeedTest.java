package com.aidanwhiteley.books.util;

import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Item;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static org.junit.Assert.*;

public class SiteRssFeedTest extends IntegrationTest {
	
    @Autowired
    SiteRssFeed siteFeed;
    
    @Value("${books.feeds.description}")
	private String booksFeedsDescription;
    
    @Value("${books.feeds.maxentries}")
	private int booksFeedsMaxEntries;
    
    @Test
    public void checkFeedForFeedData() {
        Channel channel = siteFeed.createSiteRssFeed();
        assertEquals(booksFeedsDescription, channel.getDescription());
        assertTrue(channel.getItems().size() > 0 && channel.getItems().size() <= booksFeedsMaxEntries);

        Item item = channel.getItems().get(0);
        assertFalse(item.getContent().getValue().isEmpty());
    }

}
