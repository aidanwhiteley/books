package com.aidanwhiteley.books.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import com.aidanwhiteley.books.domain.Book;

public interface BookRepository extends MongoRepository<Book, String> {

    Page<Book> findAllByAuthorOrderByEnteredDesc(Pageable page, String author);

    List<Book> findAllByGenreOrderByEnteredDesc(@Param("genre") String genre);

    Page<Book> findAllByOrderByEnteredDesc(Pageable page);

    Page<Book> findByRatingOrderByEnteredDesc(Pageable page, Book.Rating rating);
    
    long countByRating(Book.Rating rating);
}
