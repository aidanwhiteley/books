package com.aidanwhiteley.books.repository.dtos;

import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class GoogleBookSearch {

    private String title;
    private String author;
    private BookSearchResult bookSearchResult;
    private LocalDateTime expireAt;
}
