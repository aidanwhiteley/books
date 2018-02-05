package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.repository.dtos.BooksByAuthor;
import com.aidanwhiteley.books.repository.dtos.BooksByGenre;
import com.aidanwhiteley.books.repository.dtos.BooksByRating;
import com.aidanwhiteley.books.repository.dtos.BooksByReader;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookRepositoryCustomMethods {

    List<BooksByGenre> countBooksByGenre();
    List<BooksByRating> countBooksByRating();
    List<BooksByAuthor> countBooksByAuthor();
    List<BooksByReader> countBooksByReader();
   
}
