package com.aidanwhiteley.books.repository.dtos;

import com.aidanwhiteley.books.domain.Book;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class BooksByRating implements Comparable {

    private Book.Rating rating;
    private long countOfBooks;

    /**
     * We want these sorted from best to worst.
     *
     * @param o
     * @return
     */
    @Override
    public int compareTo(Object o) {
        if (o instanceof BooksByRating) {

            BooksByRating other = (BooksByRating) o;

            if (rating.getRatingLevel() == other.getRating().getRatingLevel()) {
                return 0;
            } else if (rating.getRatingLevel() > other.getRating().getRatingLevel()) {
                return 1;
            } else {
                return -1;
            }
        } else {
            throw new IllegalArgumentException("Not comparing a BooksByRating");
        }
    }
}
