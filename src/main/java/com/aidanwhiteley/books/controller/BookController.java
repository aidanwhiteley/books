package com.aidanwhiteley.books.controller;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.repository.BookRepository;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@RestController
@RequestMapping("/api")
public class BookController {

	@Autowired
	private BookRepository bookRepository;

    @RequestMapping(value = "/books/{id}", method = GET)
    public Book findBookById(@PathVariable("id") String id) {
        return bookRepository.findOne(id);
    }

	@RequestMapping(value = "/books", method = GET)
	public List<Book> findByAuthor(@RequestParam("author") String author) {
		return bookRepository.findByAuthor(author);
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

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(updatedBook.getId()).toUri();

        return ResponseEntity.noContent().build();
    }
}
