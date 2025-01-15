package com.aidanwhiteley.books.controller.dtos;

import com.aidanwhiteley.books.domain.Comment;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
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
