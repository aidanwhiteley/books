package com.aidanwhiteley.books.controller.dtos;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentForm {

    @Size(min = 10, max = 100, message
            = "A review comment must be between 10 and 1000 characters")
    private String comment;
    private String bookId;

}
