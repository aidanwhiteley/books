package com.aidanwhiteley.books.service;

import com.aidanwhiteley.books.repository.SitemapDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SitemapService {

    private static final String SITEMAP_XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String SITEMAP_XML_NAMESPACE = "http://www.sitemaps.org/schemas/sitemap/0.9";
    private static final DateTimeFormatter LAST_MODIFIED_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final List<String> STATIC_PATHS = List.of("/", "/recent", "/find", "/statistics", "/privacy", "/tandcs");

    private final SitemapDao sitemapDao;
    private final String siteDomain;

    public SitemapService(SitemapDao sitemapDao, @Value("${books.feeds.domain}") String siteDomain) {
        this.sitemapDao = sitemapDao;
        this.siteDomain = siteDomain;
    }

    public String createSitemapXml() {
        StringBuilder xml = new StringBuilder();
        xml.append(SITEMAP_XML_HEADER)
                .append("\n<urlset xmlns=\"")
                .append(SITEMAP_XML_NAMESPACE)
                .append("\">");

        STATIC_PATHS.forEach(path -> appendUrl(xml, createAbsoluteUrl(path), null));
        sitemapDao.findBooksForSitemap()
                .forEach(book -> appendUrl(xml, createBookReviewUrl(book.id()), book.lastModifiedDateTime()));

        xml.append("\n</urlset>");
        return xml.toString();
    }

    private void appendUrl(StringBuilder xml, String location, LocalDateTime lastModifiedDateTime) {
        xml.append("\n  <url>")
                .append("\n    <loc>").append(escapeXml(location)).append("</loc>");

        if (lastModifiedDateTime != null) {
            xml.append("\n    <lastmod>")
                    .append(lastModifiedDateTime.toLocalDate().format(LAST_MODIFIED_FORMATTER))
                    .append("</lastmod>");
        }

        xml.append("\n  </url>");
    }

    private String createAbsoluteUrl(String path) {
        return UriComponentsBuilder.fromUriString(getBaseDomain())
                .path(path)
                .build()
                .toUriString();
    }

    private String createBookReviewUrl(String bookId) {
        return UriComponentsBuilder.fromUriString(getBaseDomain())
                .path("/bookreview")
                .queryParam("bookId", bookId)
                .build()
                .toUriString();
    }

    private String getBaseDomain() {
        return siteDomain.endsWith("/") ? siteDomain.substring(0, siteDomain.length() - 1) : siteDomain;
    }

    static String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}


