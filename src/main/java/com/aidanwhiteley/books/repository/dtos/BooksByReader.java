package com.aidanwhiteley.books.repository.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class BooksByReader {

    private String reader;
    private long countOfBooks;
}
