package com.aidanwhiteley.books.repository.dtos;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BooksByReader {

    private String reader;
    private long countOfBooks;
}
