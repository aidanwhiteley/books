package com.aidanwhiteley.books.repository.dtos;

import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class GoogleBookSearch {

    @Id
    private String id = UUID.randomUUID().toString();
    private String title;
    private String author;
    private BookSearchResult bookSearchResult;
    private LocalDateTime expireAt;

    public GoogleBookSearch(String title, String author, BookSearchResult bookSearchResult, LocalDateTime expireAt) {
        this.title = title;
        this.author = author;
        this.bookSearchResult = bookSearchResult;
        this.expireAt = expireAt;
    }
}
