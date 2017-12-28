package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping("/api")
public class BookController {

    @Autowired
    private BookRepository bookRepository;

    @RequestMapping(value = "/books/{id}", method = GET)
    public Book findBookById(@PathVariable("id") String id) {
        return bookRepository.findOne(id);
    }

    @RequestMapping(value = "/books", method = GET, params = "author")
    public List<Book> findByAuthor(@RequestParam("author") String author) {
        return bookRepository.findAllByAuthor(author);
    }

    @RequestMapping(value = "/books", method = GET, params = "genre")
    public List<Book> findByGenre(@RequestParam("genre") String genre) {
        return bookRepository.findAllByGenre(genre);
    }

    @RequestMapping(value = "/books", method = POST)
    public ResponseEntity<?> createBook(@RequestBody Book book) {

        Book insertedBook = bookRepository.insert(book);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(insertedBook.getId()).toUri();

        return ResponseEntity.created(location).build();
    }

    @RequestMapping(value = "/books", method = PUT)
    public ResponseEntity<?> updateBook(@RequestBody Book book) {

        Book updatedBook = bookRepository.save(book);

        return ResponseEntity.noContent().build();
    }


}
