package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.util.SiteRssFeed;
import com.rometools.rome.feed.rss.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feeds")
public class FeedsController {

    private final SiteRssFeed siteRssFeed;

    @Autowired
    public FeedsController(SiteRssFeed siteRssFeed) {
        this.siteRssFeed = siteRssFeed;
    }

    @GetMapping(value = "/rss")
    public Channel findRecentActivity() {
        return siteRssFeed.createSiteRssFeed();
    }
}
