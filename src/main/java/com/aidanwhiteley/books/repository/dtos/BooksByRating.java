package com.aidanwhiteley.books.repository.dtos;

import com.aidanwhiteley.books.domain.Book;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (countOfBooks ^ (countOfBooks >>> 32));
        result = prime * result + ((rating == null) ? 0 : rating.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BooksByRating other = (BooksByRating) obj;
        return countOfBooks == other.countOfBooks && rating == other.rating;
    }

}
