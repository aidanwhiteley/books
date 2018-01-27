package com.aidanwhiteley.books.controller;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.Owner;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.GoogleBooksDao;
import com.aidanwhiteley.books.util.AuthenticationUtils;

@RestController
@RequestMapping("/secure/api")
@PreAuthorize("hasAnyRole('ROLE_EDITOR', 'ROLE_ADMIN')")
public class BookSecureController {

	public static final String CREATED_BY_DELIMETER = "|";

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private GoogleBooksDao googleBooksDao;

	@Autowired
	private AuthenticationUtils authUtils;

	@RequestMapping(value = "/books", method = POST)
	public ResponseEntity<?> createBook(@Valid @RequestBody Book book, Principal principal, HttpServletRequest request)
			throws MalformedURLException, URISyntaxException {

		User user = authUtils.extractUserFromPrincipal(principal);

		book.setCreatedBy(new Owner(user));

		// Get the Google book details for this book
		// TODO - move this out to a message queue driven async implementation.
		if (book.getGoogleBookId() != null && book.getGoogleBookId().length() > 0) {
			book.setGoogleBookDetails(googleBooksDao.searchGoogleBooksByGoogleBookId(book.getGoogleBookId()));
		}

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

}
