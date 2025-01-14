package com.aidanwhiteley.books.controller.dtos;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.Comment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BookForm {

    private List<Comment> comments = new ArrayList<>();
    private String title;
    private String author;
    private String genre;
    private String summary;
    private String rating;
    private String googleBookId;
}
