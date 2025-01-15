package com.aidanwhiteley.books.controller.dtos;

import com.aidanwhiteley.books.domain.Comment;

import java.util.List;

public record BookFormRecord(String author, String genre, String rating, String summary, String googleBookId, List<Comment> comments) {
}
