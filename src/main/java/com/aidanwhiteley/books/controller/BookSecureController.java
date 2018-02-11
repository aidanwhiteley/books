package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.aspect.LimitDataVisibility;
import com.aidanwhiteley.books.controller.util.LimitBookDataVisibility;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.Comment;
import com.aidanwhiteley.books.domain.Owner;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.GoogleBooksDao;
import com.aidanwhiteley.books.repository.dtos.BooksByReader;
import com.aidanwhiteley.books.util.AuthenticationUtils;
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
import java.util.List;
import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@LimitDataVisibility
@RestController
@RequestMapping("/secure/api")
@PreAuthorize("hasAnyRole('ROLE_EDITOR', 'ROLE_ADMIN')")
public class BookSecureController {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private GoogleBooksDao googleBooksDao;

    @Autowired
    private AuthenticationUtils authUtils;

    @Value("${books.users.default.page.size}")
    private int defaultPageSize;


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
    public ResponseEntity<?> updateBook(@Valid @RequestBody Book book, Principal principal) {

        User user = authUtils.extractUserFromPrincipal(principal);
        Book currentBookState = bookRepository.findOne(book.getId());

        if (currentBookState.isOwner(user) || user.getRoles().contains(User.Role.ROLE_ADMIN)) {
            bookRepository.save(book);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }


    @RequestMapping(value = "/books/{id}", method = DELETE)
    public ResponseEntity<?> deleteBookById(@PathVariable("id") String id, Principal principal) {

        User user = authUtils.extractUserFromPrincipal(principal);
        Book currentBookState = bookRepository.findOne(id);

        if (currentBookState.isOwner(user) || user.getRoles().contains(User.Role.ROLE_ADMIN)) {
            bookRepository.delete(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @RequestMapping(value = "/books/{id}/comments", method = POST)
    public Book addCommentToBook(@PathVariable("id") String id, @Valid @RequestBody Comment comment, Principal principal) {

        User user = authUtils.extractUserFromPrincipal(principal);

        comment.setOwner(new Owner(user));
        Book currentBookState = bookRepository.findOne(id);

        return bookRepository.addCommentToBook(id, comment);
    }

    @RequestMapping(value = "/books/{id}/comments/{commentId}", method = DELETE)
    public Book removeCommentFromBook(@PathVariable("id") String id, @PathVariable("commentId") String commentId, Principal principal) {

        User user = authUtils.extractUserFromPrincipal(principal);

        Book currentBook = bookRepository.findOne(id);
        Comment comment = currentBook.getComments().stream().filter(c-> c.getId().equals(commentId)).findFirst().orElse(null);
        if (comment == null) {
            throw new IllegalArgumentException();
        }

        if (comment.isOwner(user) || user.getRoles().contains(User.Role.ROLE_ADMIN)) {
            return bookRepository.removeCommentFromBook(id, commentId, user.getFullName());
        } else {
            throw new AccessForbiddenException();
        }
    }


    /**
     * This method is secured so that it cant be called to try and find out who has been reviewing book
     * if you are not an authorised user i.e. with at least ROLE_EDITOR
     * @param genre
     * @param page
     * @param size
     * @param principal
     * @return
     */
    @GetMapping(value = "/books", params = "reader")
    public Page<Book> findByReader(@RequestParam("reader") String genre, @RequestParam(value = "page") Optional<Integer> page, @RequestParam(value = "size") Optional<Integer> size, Principal principal) {

        PageRequest pageObj = new PageRequest(page.orElse(0),
                size.orElse(defaultPageSize), new Sort(Sort.Direction.DESC, "entered"));

        return bookRepository.findByReaderOrderByEnteredDesc(pageObj, genre);
    }


    @RequestMapping(value = "/books/readers", method = GET)
    public List<BooksByReader> findBookReaders() {
        return bookRepository.countBooksByReader();
    }

    @SuppressWarnings("serial")
    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    class AccessForbiddenException extends RuntimeException {
    }

    @SuppressWarnings("serial")
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    class IllegalArgumentException extends RuntimeException {
    }

}
