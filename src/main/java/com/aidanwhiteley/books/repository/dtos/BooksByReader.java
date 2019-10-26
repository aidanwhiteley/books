package com.aidanwhiteley.books.repository.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BooksByReader {

    private String reader;
    private long countOfBooks;
}
