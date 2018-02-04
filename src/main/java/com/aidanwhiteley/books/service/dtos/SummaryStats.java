package com.aidanwhiteley.books.service.dtos;

import com.aidanwhiteley.books.repository.dtos.BooksByGenre;
import com.aidanwhiteley.books.repository.dtos.BooksByRating;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class SummaryStats {

    private long count;
    private List<BooksByRating> booksByRating;
    private List<BooksByGenre> bookByGenre;

}
