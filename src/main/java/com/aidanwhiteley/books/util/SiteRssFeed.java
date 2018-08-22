package com.aidanwhiteley.books.util;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.repository.BookRepository;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

@Component
public class SiteRssFeed {

	private static final String FEED_TYPE_RSS_1_0 = "rss_1.0";

	@Value("${books.feeds.maxentries}")
	private int booksFeedsMaxEntries;

	@Value("${books.feeds.title}")
	private String booksFeedsTitles;

	@Value("${books.feeds.domain}")
	private String booksFeedsDomain;

	@Value("${books.feeds.description}")
	private String booksFeedsDescription;

	private final BookRepository bookRepository;

	@Autowired
	public SiteRssFeed(BookRepository bookRepository) {
		this.bookRepository = bookRepository;
	}

	SyndFeed createSiteRssFeed() {
		PageRequest pageObj = PageRequest.of(1, booksFeedsMaxEntries);
		Page<Book> recentBooks = bookRepository.findAllByOrderByCreatedDateTimeDesc(pageObj);

		SyndFeed feed = new SyndFeedImpl();
		feed.setFeedType(FEED_TYPE_RSS_1_0);
		feed.setTitle(booksFeedsTitles);
		feed.setLink(booksFeedsDomain);
		feed.setDescription(booksFeedsDescription);

		feed.setEntries(recentBooks.stream().map(b -> {
			SyndEntry entry = new SyndEntryImpl();
			entry.setTitle(b.getTitle());
			entry.setLink(booksFeedsDomain + "/" + b.getId());
			SyndContent description = new SyndContentImpl();
			description.setType("text/html");
			description.setValue(b.getSummary());
			entry.setDescription(description);
			return entry;
		}).collect(Collectors.toList()));

		return feed;
	}
}
