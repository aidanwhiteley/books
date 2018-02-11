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
	 *
	 * @param other
	 * @return
	 */
	@Override
	public int compareTo(BooksByRating other) {

		if (rating.getRatingLevel() == other.getRating().getRatingLevel()) {
			return 0;
		} else if (rating.getRatingLevel() > other.getRating().getRatingLevel()) {
			return 1;
		} else {
			return -1;
		}

	}
}
