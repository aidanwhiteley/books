package com.aidanwhiteley.books.service;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import com.aidanwhiteley.books.domain.googlebooks.VolumeInfo;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.GoogleBookSearchRepository;
import com.aidanwhiteley.books.repository.GoogleBooksDaoSync;
import com.aidanwhiteley.books.repository.dtos.GoogleBookSearch;
import com.aidanwhiteley.books.service.dtos.GoogleBookSearchResult;
import com.aidanwhiteley.books.util.HtmlSanitiserUtils;
import com.aidanwhiteley.books.util.LogDetaint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GoogleBookSearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleBookSearchService.class);

    private final GoogleBookSearchRepository googleBookSearchRepository;
    private final GoogleBooksDaoSync googleBooksDaoSync;
    private final BookRepository bookRepository;

    @Value("${books.google.books.cacheTimeoutMinutes}")
    private int cacheTimeoutMinutes;

    public GoogleBookSearchService(GoogleBookSearchRepository googleBookSearchRepository,
                                   GoogleBooksDaoSync googleBooksDaoSync,
                                   BookRepository bookRepository) {
        this.googleBookSearchRepository = googleBookSearchRepository;
        this.googleBooksDaoSync = googleBooksDaoSync;
        this.bookRepository =bookRepository;
    }

    public GoogleBookSearchResult getGoogleBooks(String title, String author, int index) {

        List<GoogleBookSearch> googleBookSearchList = googleBookSearchRepository.findAllByTitleAndAuthor(title, author);

        if (!googleBookSearchList.isEmpty()) {
            return getGoogleBookSearchResultFromCache(title, author, index, googleBookSearchList);
        } else {
            return getGoogleBookSearchResultFromAPI(title, author, index);
        }
    }

    public Book updateBookWithGoogleBookDetails(Book aBook, String title, String author, int index) {

        GoogleBookSearchResult googleBookSearchResult = getGoogleBooks(title, author, index);
        Item item = googleBookSearchResult.getItem();

        // Google Books API data _should_ be safe from CSRF attacks but lets make sure before storing the
        // description text in the database!
        VolumeInfo vlInfo = item.getVolumeInfo();
        if (vlInfo != null && vlInfo.getDescription() != null) {
            vlInfo.setDescription(HtmlSanitiserUtils.allowBasicTextFormattingOnly(vlInfo.getDescription()));
            item.setVolumeInfo(vlInfo);
        }

        bookRepository.addGoogleBookItemToBook(aBook.getId(), item);
        LOGGER.debug("Google Books details added to Mongo for {}", aBook.getId());

        aBook.setGoogleBookDetails(item);
        return aBook;
    }


    private GoogleBookSearchResult getGoogleBookSearchResultFromCache(String title, String author, int index,
                                                                      List<GoogleBookSearch> googleBookSearchList) {
        LOGGER.debug("Using Google books search cache for title {}, author {} and index {} ",
                LogDetaint.logMessageDetaint(title),
                LogDetaint.logMessageDetaint(author),
                LogDetaint.logMessageDetaint(index));
        // A unique index means that there should only be one entry
        GoogleBookSearch googleBookSearch = googleBookSearchList.getFirst();
        Item anItem = googleBookSearch.getBookSearchResult().getItems().get(index);
        boolean hasMore = index < googleBookSearch.getBookSearchResult().getItems().size() - 1;
        boolean hasPrevious = index > 0;
        return new GoogleBookSearchResult(anItem, index, hasMore, hasPrevious, true);
    }

    private GoogleBookSearchResult getGoogleBookSearchResultFromAPI(String title, String author, int index) {
        LOGGER.debug("Calling Google books API for title {}, author {} and index {} ",
                LogDetaint.logMessageDetaint(title),
                LogDetaint.logMessageDetaint(author),
                LogDetaint.logMessageDetaint(index));
        if (index != 0) {
            LOGGER.warn("Calling the Google books API when the required index is {}. This is unusual " +
                    "and probably either indicates a logic error in the client or that the timeout on the " +
                    "Google books search cache is too low at {} minutes", index, cacheTimeoutMinutes);
        }

        BookSearchResult result = googleBooksDaoSync.searchGoogleBooksByTitleAndAuthor(title, author);
        if (!result.getItems().isEmpty()) {
            LOGGER.debug("Inserting an entry into the Google books search cache for title {}, author {} and index {} ",
                    LogDetaint.logMessageDetaint(title),
                    LogDetaint.logMessageDetaint(author),
                    LogDetaint.logMessageDetaint(index));
            googleBookSearchRepository.insert(
                    new GoogleBookSearch(title, author, result, LocalDateTime.now().plusMinutes(cacheTimeoutMinutes)));

            Item anItem = index < result.getItems().size() ? result.getItems().get(index) :
                    result.getItems().getLast();
            boolean hasMore = index < result.getItems().size() - 1;
            boolean hasPrevious = index > 0;
            return new GoogleBookSearchResult(anItem, index, hasMore, hasPrevious, false);
        } else {
            LOGGER.debug("No matching results found using the Google Books API for title {}, author {} and index {} ",
                    LogDetaint.logMessageDetaint(title),
                    LogDetaint.logMessageDetaint(author),
                    LogDetaint.logMessageDetaint(index));
            return new GoogleBookSearchResult(null, index, false, false, false);
        }
    }
}
