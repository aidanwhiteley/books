package com.aidanwhiteley.books.repository.dtos;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BooksByGenre {

    private String genre;
    private long countOfBooks;
}
