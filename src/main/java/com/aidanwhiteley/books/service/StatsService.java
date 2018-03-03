package com.aidanwhiteley.books.service;

import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.dtos.BooksByRating;
import com.aidanwhiteley.books.service.dtos.SummaryStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class StatsService {

    private final BookRepository bookRepository;

    @Autowired
    public StatsService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public SummaryStats getSummaryStats() {

        List<BooksByRating> booksByRatingList = bookRepository.countBooksByRating();

        List<BooksByRating> modifiableList = new ArrayList<>(booksByRatingList);
        modifiableList.sort(Comparator.reverseOrder());

        return SummaryStats.builder().
                count(bookRepository.count()).
                bookByGenre(bookRepository.countBooksByGenre()).
                booksByRating(modifiableList).
                build();
    }

}
