package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.aspect.LimitDataVisibility;
import com.aidanwhiteley.books.controller.dtos.CommentRec;
import com.aidanwhiteley.books.controller.exceptions.NotAuthorisedException;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.Comment;
import com.aidanwhiteley.books.domain.Owner;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.GoogleBooksDaoAsync;
import com.aidanwhiteley.books.repository.GoogleBooksDaoSync;
import com.aidanwhiteley.books.repository.dtos.BooksByReader;
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

import static com.aidanwhiteley.books.util.LogDetaint.logMessageDetaint;

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

    @Value("${books.users.max.page.size}")
    private int maxPageSize;

    public BookSecureController(BookRepository bookRepository, GoogleBooksDaoSync googleBooksDaoSync,
                                GoogleBooksDaoAsync googleBooksDaoAsync, JwtAuthenticationUtils jwtAuthenticationUtils) {
        this.bookRepository = bookRepository;
        this.googleBooksDaoSync = googleBooksDaoSync;
        this.googleBooksDaoAsync = googleBooksDaoAsync;
        this.authUtils = jwtAuthenticationUtils;
    }

    protected static Book mergeUpdatesOntoExistingBook(Book currentBookState, Book book) {

        // Set the fields the owner / admin is allowed to manually update
        currentBookState.setSummary(book.getSummary());
        currentBookState.setGenre(book.getGenre());
        currentBookState.setTitle(book.getTitle());
        currentBookState.setSummary(book.getSummary());
        currentBookState.setGoogleBookId(book.getGoogleBookId());
        currentBookState.setRating(book.getRating());
        currentBookState.setAuthor(book.getAuthor());

        return currentBookState;
    }

    @PostMapping(value = "/books")
    public ResponseEntity<Book> createBook(@Valid @RequestBody Book book, Principal principal) throws MalformedURLException, URISyntaxException {

        LOGGER.debug("createBook in BookSecureController called");

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {

            Book insertedBook = bookRepository.insert(book);

            // If there were Google Book details specified, call an async method to
            // go and get the full details from Google and then update the Mongo document for the book
            if (book.getGoogleBookId() != null && !book.getGoogleBookId().isEmpty()) {
                googleBooksDaoAsync.updateBookWithGoogleBookDetails(insertedBook, book.getGoogleBookId());
            }

            URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                    .buildAndExpand(insertedBook.getId()).toUri();

            // Basic GET of book details are not on a secure API
            location = new URI(location.toURL().toString().replace("/secure", ""));
            LOGGER.debug("createBook existed. New Book created in store - accessible at {}", location);
            return ResponseEntity.created(location).build();
        } else {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Couldn't create a book as user to own book not found! Principal: {}", logMessageDetaint(principal));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PutMapping(value = "/books")
    public ResponseEntity<Book> updateBook(@Valid @RequestBody Book book, Principal principal) {

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {
            Book currentBookState = bookRepository.findById(book.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Didn't find book to update"));

            if (currentBookState.isOwner(user.get()) || user.get().getRoles().contains(User.Role.ROLE_ADMIN)) {

                boolean inputHasGoogleBookId = book.getGoogleBookId() != null && (!book.getGoogleBookId().isEmpty());
                boolean currentBookHasGoogleBookId = currentBookState.getGoogleBookId() != null &&
                        (!currentBookState.getGoogleBookId().isEmpty());

                if (inputHasGoogleBookId && (currentBookHasGoogleBookId &&
                        !currentBookState.getGoogleBookId().equalsIgnoreCase(book.getGoogleBookId()))
                ) {
                    // Retrieve and update Google Book details synchronously
                    Item item = googleBooksDaoSync.searchGoogleBooksByGoogleBookId(book.getGoogleBookId());
                    currentBookState.setGoogleBookDetails(item);
                } else if (book.getGoogleBookId() == null || book.getGoogleBookId().isEmpty()) {
                    currentBookState.setGoogleBookDetails(null);
                }

                Book mergedBook = mergeUpdatesOntoExistingBook(currentBookState, book);

                bookRepository.save(mergedBook);
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping(value = "/books/{id}")
    public ResponseEntity<Book> deleteBookById(@PathVariable String id, Principal principal) {

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {
            Book currentBookState = bookRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Couldn't find book to delete"));

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

    @PostMapping(value = "/books/{id}/comments")
    public Book addCommentToBook(@PathVariable String id, @Valid @RequestBody CommentRec commentRec,
                                 Principal principal) {

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {
            Comment comment = new Comment(commentRec.commentText(), new Owner(user.get()));
            return bookRepository.addCommentToBook(id, comment);
        } else {
            return null;
        }
    }

    @DeleteMapping(value = "/books/{id}/comments/{commentId}")
    public Book removeCommentFromBook(@PathVariable String id, @PathVariable String commentId,
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
                throw new NotAuthorisedException("Not owner of comment or admin");
            }
        } else {
            return null;
        }
    }

    /**
     * This method is secured so that it cant be called to try and find out who
     * has been reviewing books if you are not an authorised user i.e. with at
     * least ROLE_EDITOR
     */
    @GetMapping(value = {"/books", "/books/"})
    public Page<Book> findByReader(@RequestParam String reader, @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "5") int size, Principal principal) {

        if (null == reader || reader.trim().isEmpty()) {
            throw new IllegalArgumentException("Reader parameter cannot be empty");
        }

        if (size > maxPageSize) {
            throw new IllegalArgumentException("Cannot request a page of data containing more that %s elements".formatted(maxPageSize));
        }

        PageRequest pageObj = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDateTime"));
        return bookRepository.findByReaderOrderByCreatedDateTimeDesc(pageObj, reader);
    }

    @GetMapping(value = {"/googlebooks", "googlebooks/"}, params = {"title", "author"})
    public BookSearchResult findGoogleBooksByTitleAndAuthor(@RequestParam String title, @RequestParam String author) {
        return googleBooksDaoSync.searchGoogleBooksByTitleAndAuthor(title, author);
    }

    @GetMapping(value = "/books/readers")
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
            headersOut.append(URLEncoder.encode(headerName, StandardCharsets.UTF_8)).
                    append(": ").append(URLEncoder.encode(request.getHeader(headerName), StandardCharsets.UTF_8)).append("\r\n");
        }

        return "Scheme was: " + URLEncoder.encode(request.getScheme(), StandardCharsets.UTF_8) +
                " servername was: " + URLEncoder.encode(request.getServerName(), StandardCharsets.UTF_8)
                + " and protocol was: " + URLEncoder.encode(request.getProtocol(), StandardCharsets.UTF_8) +
                "\r\n\r\nheaders:\r\n" + headersOut;
    }
}
