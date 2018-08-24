package com.aidanwhiteley.books.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.sun.syndication.feed.synd.SyndEntry;

import com.sun.syndication.feed.synd.SyndFeed;

public class SiteRssFeedTest extends IntegrationTest {
	
    @Autowired
    SiteRssFeed siteFeed;
    
    @Value("${books.feeds.description}")
	private String booksFeedsDescription;
    
    @Value("${books.feeds.maxentries}")
	private int booksFeedsMaxEntries;
    
    @Test
    public void checkFeedForFeedData() {
    	SyndFeed feed = siteFeed.createSiteRssFeed();
    	assertEquals(booksFeedsDescription, feed.getDescription());
    	assertTrue(feed.getEntries().size() > 0 && feed.getEntries().size() <= booksFeedsMaxEntries);
    	
    	SyndEntry entry = (SyndEntry)(feed.getEntries().get(0));
    	assertTrue(entry.getDescription().getValue().isEmpty() == false);
    }

}
