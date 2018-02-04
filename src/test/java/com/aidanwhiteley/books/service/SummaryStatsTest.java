package com.aidanwhiteley.books.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.aidanwhiteley.books.service.dtos.SummaryStats;
import com.aidanwhiteley.books.util.IntegrationTest;

public class SummaryStatsTest extends IntegrationTest {

	@Autowired
	StatsService statsService;

	@Test
	public void getSummaryStats() {

		SummaryStats stats = statsService.getSummaryStats();
		assertEquals(stats.getCount(), stats.getCountGreatBooks() + stats.getCountGoodBook() + stats.getCountOkBooks()
				+ stats.getCountPoorBooks() + stats.getCountTerribleBooks());
	}

}
