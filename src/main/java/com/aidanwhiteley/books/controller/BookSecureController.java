package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.aspect.LimitDataVisibility;
import com.aidanwhiteley.books.controller.exceptions.AccessForbiddenException;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.Comment;
import com.aidanwhiteley.books.domain.Owner;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.GoogleBooksDaoAsync;
import com.aidanwhiteley.books.repository.GoogleBooksDaoSync;
import com.aidanwhiteley.books.repository.dtos.BooksByReader;
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@LimitDataVisibility
@RestController
@RequestMapping("/secure/api")
@PreAuthorize("hasAnyRole('ROLE_EDITOR', 'ROLE_ADMIN')")
public class BookSecureController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookSecureController.class);

    private final BookRepository bookRepository;

    private final GoogleBooksDaoSync googleBooksDaoSync;

    private final GoogleBooksDaoAsync googleBooksDaoAsync;

    private final JwtAuthenticationUtils authUtils;

    @Value("${books.users.default.page.size}")
    private int defaultPageSize;

    @Autowired
    public BookSecureController(BookRepository bookRepository, GoogleBooksDaoSync googleBooksDaoSync,
                                GoogleBooksDaoAsync googleBooksDaoAsync, JwtAuthenticationUtils jwtAuthenticationUtils) {
        this.bookRepository = bookRepository;
        this.googleBooksDaoSync = googleBooksDaoSync;
        this.googleBooksDaoAsync = googleBooksDaoAsync;
        this.authUtils = jwtAuthenticationUtils;
    }

    @RequestMapping(value = "/books", method = POST)
    public ResponseEntity<Book> createBook(@Valid @RequestBody Book book, Principal principal,
                                           HttpServletRequest request) throws MalformedURLException, URISyntaxException {

        LOGGER.debug("createBook in BookSecureController called");

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {
            book.setCreatedBy(new Owner(user.get()));

            Book insertedBook = bookRepository.insert(book);

            // If there were Google Book details specified, call an async method to
            // go and get the full details from Google and then update the Mongo document for the book
            if (book.getGoogleBookId() != null && book.getGoogleBookId().length() > 0) {
                googleBooksDaoAsync.updateBookWithGoogleBookDetails(insertedBook, book.getGoogleBookId());
            }

            URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                    .buildAndExpand(insertedBook.getId()).toUri();

            // Basic GET of book details are not on a secure API
            location = new URI(location.toURL().toString().replaceAll("/secure", ""));
            LOGGER.debug("createBook existed. New Book createdd in store - accessible at {}", location);
            return ResponseEntity.created(location).build();
        } else {
            LOGGER.error("Couldnt create a book as user to own book not found! Principal: {}", principal);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @RequestMapping(value = "/books", method = PUT)
    public ResponseEntity<Book> updateBook(@Valid @RequestBody Book book, Principal principal) {

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {
            Book currentBookState = bookRepository.findById(book.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Didnt find book to update"));

            if (currentBookState.isOwner(user.get()) || user.get().getRoles().contains(User.Role.ROLE_ADMIN)) {
                // Have the Google book details for this book review changed (or
                // been removed)
                if (book.getGoogleBookId() != null && !book.getGoogleBookId().isEmpty()
                        && (!book.getGoogleBookId().equals(currentBookState.getGoogleBookId()))) {
                    // Retrieve and update Google Book details asynchronously
                    googleBooksDaoSync.searchGoogleBooksByGoogleBookId(book.getGoogleBookId());
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
    public ResponseEntity<Book> deleteBookById(@PathVariable("id") String id, Principal principal) {

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {
            Book currentBookState = bookRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Couldnt find book to delete"));

            if (currentBookState.isOwner(user.get()) || user.get().getRoles().contains(User.Role.ROLE_ADMIN)) {
                bookRepository.deleteById(id);
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
            Book currentBook = bookRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Unable to find book to delete comment from"));
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

    @GetMapping(value = "/books", params = {"reader"})
    public Page<Book> findByReader(@RequestParam("reader") String reader, Principal principal) {
        return findByReader(reader, 0, defaultPageSize, principal);
    }

    /**
     * This method is secured so that it cant be called to try and find out who
     * has been reviewing books if you are not an authorised user i.e. with at
     * least ROLE_EDITOR
     */
    @GetMapping(value = "/books", params = {"reader", "page", "size"})
    public Page<Book> findByReader(@RequestParam("reader") String reader, @RequestParam(value = "page") int page,
                                   @RequestParam(value = "size") int size, Principal principal) {

        if (null == reader || reader.trim().isEmpty()) {
            throw new IllegalArgumentException("Reader parameter cannot be empty");
        }

        PageRequest pageObj = PageRequest.of(page, size, new Sort(Sort.Direction.DESC, "entered"));
        return bookRepository.findByReaderOrderByEnteredDesc(pageObj, reader);
    }

    @GetMapping(value = "/googlebooks", params = "title")
    public BookSearchResult findGoogleBooksByTitle(@RequestParam("title") String title) {
        return googleBooksDaoSync.searchGoogBooksByTitle(title);
    }

    @RequestMapping(value = "/books/readers", method = GET)
    public List<BooksByReader> findBookReaders() {
        return bookRepository.countBooksByReader();
    }

    @GetMapping(value = "/debugheaders")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String debugRequestHeaders(Principal principal, HttpServletRequest request) {

        Enumeration<String> headers = request.getHeaderNames();
        StringBuilder headersOut = new StringBuilder();
        while (headers.hasMoreElements()) {
            String headerName = headers.nextElement();
            headersOut.append(headerName).append(": ").append(request.getHeader(headerName)).append("\r\n");
        }

        return "Scheme was: " + request.getScheme() + " servername was: " + request.getServerName()
                + " and protocol was: " + request.getProtocol() + "\r\n\r\nheaders:\r\n" + headersOut;
    }
}
