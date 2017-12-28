package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends MongoRepository<Book, String> {

    List<Book> findAllByAuthor(@Param("author") String author);

    List<Book> findAllByGenre(@Param("genre") String genre);

    List<Book> findAllOrderByRating();

    List<Book> findAllOrderByLastRead();
}
