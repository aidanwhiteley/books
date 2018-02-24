package com.aidanwhiteley.books.controller;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.aidanwhiteley.books.controller.aspect.LimitDataVisibility;
import com.aidanwhiteley.books.controller.exceptions.AccessForbiddenException;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.Comment;
import com.aidanwhiteley.books.domain.Owner;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.GoogleBooksDao;
import com.aidanwhiteley.books.repository.dtos.BooksByReader;
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;

@LimitDataVisibility
@RestController
@RequestMapping("/secure/api")
@PreAuthorize("hasAnyRole('ROLE_EDITOR', 'ROLE_ADMIN')")
public class BookSecureController {

	private static final Logger LOGGER = LoggerFactory.getLogger(BookSecureController.class);

	private final BookRepository bookRepository;

	private final GoogleBooksDao googleBooksDao;

	private final JwtAuthenticationUtils authUtils;

	@Value("${books.users.default.page.size}")
	private int defaultPageSize;

	@Autowired
	public BookSecureController(BookRepository bookRepository, GoogleBooksDao googleBooksDao,
			JwtAuthenticationUtils jwtAuthenticationUtils) {
		this.bookRepository = bookRepository;
		this.googleBooksDao = googleBooksDao;
		this.authUtils = jwtAuthenticationUtils;
	}

	@RequestMapping(value = "/books", method = POST)
	public ResponseEntity<?> createBook(@Valid @RequestBody Book book, Principal principal, HttpServletRequest request)
			throws MalformedURLException, URISyntaxException {

		Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
		if (user.isPresent()) {
			book.setCreatedBy(new Owner(user.get()));

			// Get the Google book details for this book
			// TODO - move this out to a message queue driven async
			// implementation.
			if (book.getGoogleBookId() != null && book.getGoogleBookId().length() > 0) {
				book.setGoogleBookDetails(googleBooksDao.searchGoogleBooksByGoogleBookId(book.getGoogleBookId()));
			}

			Book insertedBook = bookRepository.insert(book);

			URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
					.buildAndExpand(insertedBook.getId()).toUri();

			// Basic GET of book details are not on a secure API
			location = new URI(location.toURL().toString().replaceAll("/secure", ""));

			return ResponseEntity.created(location).build();
		} else {
			LOGGER.error("Couldnt create a book as user to own book not found! Principal: {}", principal);
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	@RequestMapping(value = "/books", method = PUT)
	public ResponseEntity<?> updateBook(@Valid @RequestBody Book book, Principal principal) {

		Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
		if (user.isPresent()) {
			Book currentBookState = bookRepository.findOne(book.getId());

			if (currentBookState.isOwner(user.get()) || user.get().getRoles().contains(User.Role.ROLE_ADMIN)) {
				// Have the Google book details for this book review changed (or
				// been removed)
				// TODO - move this out to a message queue driven async
				// implementation.
				if (book.getGoogleBookId() != null && !book.getGoogleBookId().isEmpty()
						&& book.getGoogleBookId() != currentBookState.getGoogleBookId()) {
					book.setGoogleBookDetails(googleBooksDao.searchGoogleBooksByGoogleBookId(book.getGoogleBookId()));
				} else if (book.getGoogleBookId() == null || book.getGoogleBookId().isEmpty()) {
					book.setGoogleBookDetails(null);
				}

				bookRepository.save(book);
				return ResponseEntity.noContent().build();
			} else {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
			}
		} else {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	@RequestMapping(value = "/books/{id}", method = DELETE)
	public ResponseEntity<?> deleteBookById(@PathVariable("id") String id, Principal principal) {

		Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
		if (user.isPresent()) {
			Book currentBookState = bookRepository.findOne(id);

			if (currentBookState.isOwner(user.get()) || user.get().getRoles().contains(User.Role.ROLE_ADMIN)) {
				bookRepository.delete(id);
				return ResponseEntity.noContent().build();
			} else {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
			}
		} else {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	@RequestMapping(value = "/books/{id}/comments", method = POST)
	public Book addCommentToBook(@PathVariable("id") String id, @Valid @RequestBody Comment comment,
			Principal principal) {

		Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
		if (user.isPresent()) {
			comment.setOwner(new Owner(user.get()));

			return bookRepository.addCommentToBook(id, comment);
		} else {
			return null;
		}
	}

	@RequestMapping(value = "/books/{id}/comments/{commentId}", method = DELETE)
	public Book removeCommentFromBook(@PathVariable("id") String id, @PathVariable("commentId") String commentId,
			Principal principal) {

		Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);

		if (user.isPresent()) {
			Book currentBook = bookRepository.findOne(id);
			Comment comment = currentBook.getComments().stream().filter(c -> c.getId().equals(commentId)).findFirst()
					.orElse(null);
			if (comment == null) {
				throw new IllegalArgumentException("Unknown commentId supplied");
			}

			if (comment.isOwner(user.get()) || user.get().getRoles().contains(User.Role.ROLE_ADMIN)) {
				return bookRepository.removeCommentFromBook(id, commentId, user.get().getFullName());
			} else {
				throw new AccessForbiddenException("Not owner of comment or admin");
			}
		} else {
			return null;
		}
	}

	@GetMapping(value = "/books", params = { "reader" })
	public Page<Book> findByReader(@RequestParam("reader") String reader, Principal principal) {
		return findByReader(reader, 0, defaultPageSize, principal);
	}

	/**
	 * This method is secured so that it cant be called to try and find out who
	 * has been reviewing books if you are not an authorised user i.e. with at
	 * least ROLE_EDITOR
	 */
	@GetMapping(value = "/books", params = { "reader", "page", "size" })
	public Page<Book> findByReader(@RequestParam("reader") String reader, @RequestParam(value = "page") int page,
			@RequestParam(value = "size") int size, Principal principal) {

		if (null == reader || reader.trim().isEmpty()) {
			throw new IllegalArgumentException("Reader parameter cannot be empty");
		}

		PageRequest pageObj = new PageRequest(page, size, new Sort(Sort.Direction.DESC, "entered"));
		return bookRepository.findByReaderOrderByEnteredDesc(pageObj, reader);
	}

	@GetMapping(value = "/googlebooks", params = "title")
	public BookSearchResult findGoogleBooksByTitle(@RequestParam("title") String title) {
		return googleBooksDao.searchGoogBooksByTitle(title);
	}

	@RequestMapping(value = "/books/readers", method = GET)
	public List<BooksByReader> findBookReaders() {
		return bookRepository.countBooksByReader();
	}

}
