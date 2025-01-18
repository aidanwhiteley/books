package com.aidanwhiteley.books.controller.dtos;

import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleBookSearch {

    private String title;
    private String author;
    private BookSearchResult bookSearchResult;
    private LocalDateTime expireAt;
}
