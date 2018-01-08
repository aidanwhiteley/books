package com.aidanwhiteley.books.controller;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.repository.BookRepository;

@RestController
@RequestMapping("/secure/api")
public class BookSecureController {

	@Autowired
	private BookRepository bookRepository;

	@RequestMapping(value = "/books", method = POST)
	public ResponseEntity<?> createBook(@Valid @RequestBody Book book) throws MalformedURLException, URISyntaxException {

		Book insertedBook = bookRepository.insert(book);

		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(insertedBook.getId()).toUri();
		
		// Basic GET of book details are not on a secure API
		location = new URI(location.toURL().toString().replaceAll("/secure", ""));

		return ResponseEntity.created(location).build();
	}

	@RequestMapping(value = "/books", method = PUT)
	public ResponseEntity<?> updateBook(@Valid @RequestBody Book book) {

		bookRepository.save(book);
		return ResponseEntity.noContent().build();
	}

	@RequestMapping(value = "/books/{id}", method = DELETE)
	public void deleteBookById(@PathVariable("id") String id) {
		bookRepository.delete(id);
	}

	@RequestMapping("/user")
	public Principal user(Principal principal) {
		return principal;
	}

}
