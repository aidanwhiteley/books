package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.repository.BookRepository;
import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Content;
import com.rometools.rome.feed.rss.Guid;
import com.rometools.rome.feed.rss.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

@Component
public class SiteRssFeed {

    private static final String FEED_TYPE_RSS_2_0 = "rss_2.0";
    private final BookRepository bookRepository;
    @Value("${books.feeds.maxentries}")
    private int booksFeedsMaxEntries;
    @Value("${books.feeds.title}")
    private String booksFeedsTitles;
    @Value("${books.feeds.domain}")
    private String booksFeedsDomain;
    @Value("${books.feeds.description}")
    private String booksFeedsDescription;

    public SiteRssFeed(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Channel createSiteRssFeed() {
        PageRequest pageObj = PageRequest.of(0, booksFeedsMaxEntries);
        Page<Book> recentBooks = bookRepository.findAllByOrderByCreatedDateTimeDesc(pageObj);

        Channel channel = new Channel(FEED_TYPE_RSS_2_0);
        channel.setTitle(booksFeedsTitles);
        channel.setLink(booksFeedsDomain);
        channel.setDescription(booksFeedsDescription);
        channel.setPubDate(new Date());

        channel.setItems(recentBooks.stream().map(b -> {
            Item item = new Item();
            item.setTitle(b.getTitle() + " by " + b.getAuthor());
            item.setLink(booksFeedsDomain + "bookreview?bookId=" + b.getId());

            Guid guid = new Guid();
            guid.setPermaLink(false);
            guid.setValue(b.getId());
            item.setGuid(guid);

            ZonedDateTime zdt = b.getCreatedDateTime().atZone(ZoneId.systemDefault());
            item.setPubDate(Date.from(zdt.toInstant()));

            Content content = new Content();
            content.setType("text/html");
            content.setValue(b.getSummary() + "\r\n \r\nRating: " + b.getRating());

            item.setContent(content);

            return item;
        }).toList());

        return channel;
    }
}
