package com.aidanwhiteley.books.repository.dtos;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BooksByAuthor {

    private String author;
    private long countOfBooks;
}
