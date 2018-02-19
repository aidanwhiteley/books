package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.Comment;
import com.aidanwhiteley.books.repository.dtos.BooksByAuthor;
import com.aidanwhiteley.books.repository.dtos.BooksByGenre;
import com.aidanwhiteley.books.repository.dtos.BooksByRating;
import com.aidanwhiteley.books.repository.dtos.BooksByReader;

import java.util.List;

public interface BookRepositoryCustomMethods {

    List<BooksByGenre> countBooksByGenre();

    List<BooksByRating> countBooksByRating();

    List<BooksByAuthor> countBooksByAuthor();

    List<BooksByReader> countBooksByReader();

    Book findCommentsForBook(String bookId);

    Book addCommentToBook(String bookId, Comment comment);

    /**
     * Marks a comment as deleted. Empties the comment text and  marks comment as deleted.
     * Doesnt actually remove the Comment from the database.
     *
     * @return Returns a Book with JUST the comments and the bookId populated
     */
    Book removeCommentFromBook(String bookId, String commentId, String removerName);
}
