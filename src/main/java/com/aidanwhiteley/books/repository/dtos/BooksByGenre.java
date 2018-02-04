package com.aidanwhiteley.books.repository.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class BooksByGenre {

    private String genre;
    private long countOfBooks;
}
