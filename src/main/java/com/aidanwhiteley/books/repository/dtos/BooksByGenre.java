package com.aidanwhiteley.books.repository.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BooksByGenre {

    private String genre;
    private long countOfBooks;
}
