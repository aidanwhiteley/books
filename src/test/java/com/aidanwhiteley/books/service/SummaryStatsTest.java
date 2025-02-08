package com.aidanwhiteley.books.service;

import com.aidanwhiteley.books.repository.dtos.BooksByGenre;
import com.aidanwhiteley.books.service.dtos.SummaryStats;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SummaryStatsTest extends IntegrationTest {

    @Autowired
    private StatsService statsService;

    @Test
    void getSummaryStats() {

        SummaryStats stats = statsService.getSummaryStats();
        List<BooksByGenre> books = stats.getBookByGenre();
        long count = books.stream().mapToLong(BooksByGenre::getCountOfBooks).sum();

        assertEquals(stats.getCount(), count);
    }
}
