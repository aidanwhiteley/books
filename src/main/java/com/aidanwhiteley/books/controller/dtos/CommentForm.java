package com.aidanwhiteley.books.controller.dtos;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.Comment;
import com.aidanwhiteley.books.service.dtos.GoogleBookSearchResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentForm {

    @Size(min = 10, max = 100, message
            = "A review comment must be between 10 and 1000 characters")
    private String comment;
    private String bookId;

}
