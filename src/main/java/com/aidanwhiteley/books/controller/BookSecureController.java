package com.aidanwhiteley.books.controller;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(BookSecureController.class);

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private UserRepository userRepository;

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
	public User user(Principal principal, HttpServletResponse response) {

		OAuth2Authentication auth = (OAuth2Authentication) principal;
		String authenticationProviderId = (String) auth.getUserAuthentication().getPrincipal();
		List<User> users = userRepository.findAllByAuthenticationServiceId(authenticationProviderId);

		if (users.size() > 0) {
			return users.get(0);
		} else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			LOGGER.info("Did not find user with auth id of {}", authenticationProviderId);
			return null;
		}
	}

}
