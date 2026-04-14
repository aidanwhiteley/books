package com.aidanwhiteley.books.service;

import com.aidanwhiteley.books.repository.SitemapDao;
import com.aidanwhiteley.books.repository.dtos.SitemapBook;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SitemapServiceTest {

    @Test
    void createSitemapXmlIncludesStaticAndBookUrls() {
        SitemapDao sitemapDao = mock(SitemapDao.class);
        when(sitemapDao.findBooksForSitemap()).thenReturn(List.of(
                new SitemapBook("book-123", LocalDateTime.of(2026, 4, 14, 9, 30)),
                new SitemapBook("book & 456", LocalDateTime.of(2026, 4, 13, 11, 45))));

        SitemapService sitemapService = new SitemapService(sitemapDao, "https://cloudybookclub.com/");

        String xml = sitemapService.createSitemapXml();

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(xml.contains("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"));
        assertTrue(xml.contains("<loc>https://cloudybookclub.com/</loc>"));
        assertTrue(xml.contains("<loc>https://cloudybookclub.com/recent</loc>"));
        assertTrue(xml.contains("<loc>https://cloudybookclub.com/bookreview?bookId=book-123</loc>"));
        assertTrue(xml.contains("<loc>https://cloudybookclub.com/bookreview?bookId=book"));
        assertTrue(xml.contains("456</loc>"));
        assertTrue(xml.contains("<lastmod>2026-04-14</lastmod>"));
        assertTrue(xml.contains("<lastmod>2026-04-13</lastmod>"));
    }
}



