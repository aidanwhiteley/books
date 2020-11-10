package com.aidanwhiteley.books.service;

import static org.junit.Assert.assertEquals;

import com.aidanwhiteley.books.repository.dtos.BooksByGenre;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.aidanwhiteley.books.service.dtos.SummaryStats;
import com.aidanwhiteley.books.util.IntegrationTest;

import java.util.List;

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
