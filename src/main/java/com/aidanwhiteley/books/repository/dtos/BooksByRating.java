package com.aidanwhiteley.books.repository.dtos;

import com.aidanwhiteley.books.domain.Book;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BooksByRating implements Comparable<BooksByRating> {

    private Book.Rating rating;
    private long countOfBooks;

    /**
     * We want these sorted from best to worst.
     */
    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(BooksByRating other) {
        return Integer.compare(rating.getRatingLevel(), other.getRating().getRatingLevel());
    }

}
