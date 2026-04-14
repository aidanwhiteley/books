package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.repository.dtos.SitemapBook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SitemapDao {

    private final BookRepository bookRepository;

    public SitemapDao(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<SitemapBook> findBooksForSitemap() {
        return bookRepository.findAll(Sort.by(Sort.Direction.DESC, "createdDateTime"))
                .stream()
                .map(book -> new SitemapBook(
                        book.getId(),
                        book.getLastModifiedDateTime() != null ? book.getLastModifiedDateTime() : book.getCreatedDateTime()))
                .toList();
    }
}

