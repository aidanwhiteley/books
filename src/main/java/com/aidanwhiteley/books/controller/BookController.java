package com.aidanwhiteley.books.controller;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.repository.BookRepository;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping("/api")
public class BookController {

	@Autowired
	private BookRepository bookRepository;

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
}
