package com.aidanwhiteley.books.repository.dtos;

import com.aidanwhiteley.books.domain.Book;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class BooksByRating implements Comparable<BooksByRating> {

	private Book.Rating rating;
	private long countOfBooks;

	/**
	 * We want these sorted from best to worst.
	 */
	@Override
	public int compareTo(BooksByRating other) {

		return Integer.compare(rating.getRatingLevel(), other.getRating().getRatingLevel());

	}
}
