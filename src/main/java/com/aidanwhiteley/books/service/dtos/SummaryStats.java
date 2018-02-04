package com.aidanwhiteley.books.service.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class SummaryStats {
	
	private long count;
	private long countGreatBooks;
	private long countGoodBook;
	private long countOkBooks;
	private long countPoorBooks;
	private long countTerribleBooks;
	
}
