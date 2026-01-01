package com.aidanwhiteley.books.service;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.util.GoodReadsBookExport;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GoodReadsExportService {
    private final BookRepository bookRepository;

    public GoodReadsExportService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<String> getExportInGoodReadsFormat(User user) {
        var output = new ArrayList<String>();
        output.add(GoodReadsBookExport.goodReadsExportHeaderRow());

        // TODO - with small volumes of book reviews this filter at the service tier is fine but
        // should be moved to being a JPA "example" query at some point
        var books = user.getHighestRole().getShortName().equals("ADMIN") ?
            bookRepository.findAll().stream().toList() :
                bookRepository.findAll().stream().filter(s -> s.isOwner(user)).toList();
        output.addAll(books.stream().map(GoodReadsBookExport::goodReadsExportAsCsv).toList());
        return output;
    }
}
