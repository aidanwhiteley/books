package com.aidanwhiteley.books.repository.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class BooksByAuthor {

    private String author;
    private long countOfBooks;
}
