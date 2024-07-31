package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.aspect.LimitDataVisibility;
import com.aidanwhiteley.books.controller.exceptions.NotFoundException;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.dtos.BooksByAuthor;
import com.aidanwhiteley.books.repository.dtos.BooksByGenre;
import com.aidanwhiteley.books.service.StatsService;
import com.aidanwhiteley.books.service.dtos.SummaryStats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@LimitDataVisibility
@RestController
@RequestMapping("/api")
public class BookController {

    public static final String PAGE_REQUEST_TOO_BIG_MESSAGE = "Cannot request a page of data containing more that %s elements";

    private final BookRepository bookRepository;

    private final StatsService statsService;

    @Value("${books.users.default.page.size}")
    private int defaultPageSize;

    @Value("${books.users.max.page.size}")
    private int maxPageSize;

    public BookController(BookRepository bookRepository, StatsService statsService) {
        this.bookRepository = bookRepository;
        this.statsService = statsService;
    }

    @GetMapping(value = "/books")
    public Page<Book> findAllByCreatedDateTimeDesc(Principal principal) {
        return findAllByCreatedDateTimeDesc(0, defaultPageSize, principal);
    }

    @GetMapping(value = {"/books", "/books/"}, params = {"page", "size"})
    public Page<Book> findAllByCreatedDateTimeDesc(@RequestParam int page,
                                                   @RequestParam int size, Principal principal) {

        PageRequest pageObj = PageRequest.of(page, size);
        return bookRepository.findAllByOrderByCreatedDateTimeDesc(pageObj);
    }

    @GetMapping(value = "/books/{id}")
    public Book findBookById(@PathVariable String id, Principal principal) {
        return bookRepository.findById(id).orElseThrow(() -> new NotFoundException("Book id " + id + " not found"));
    }

    @GetMapping(value = {"/books", "/books/"}, params = {"author"})
    public Page<Book> findByAuthor(@RequestParam String author, Principal principal) {
        return findByAuthor(author, 0, defaultPageSize, principal);
    }

    @GetMapping(value = {"/books", "books/"}, params = {"author", "page", "size"})
    public Page<Book> findByAuthor(@RequestParam String author, @RequestParam int page,
                                   @RequestParam int size, Principal principal) {

        if (null == author || author.trim().isEmpty()) {
            throw new IllegalArgumentException("Author parameter cannot be empty");
        }

        if (size > maxPageSize) {
            throw new IllegalArgumentException(PAGE_REQUEST_TOO_BIG_MESSAGE.formatted(maxPageSize));
        }

        PageRequest pageObj = PageRequest.of(page, size);
        return bookRepository.findAllByAuthorOrderByCreatedDateTimeDesc(pageObj, author);
    }

    @GetMapping(value = {"/books", "/books/"}, params = {"search"})
    public Page<Book> findBySearch(@RequestParam String search, Principal principal) {
        return findBySearch(search, 0, defaultPageSize, principal);
    }

    @GetMapping(value = {"/books", "/books/"}, params = {"search", "page", "size"})
    public Page<Book> findBySearch(@RequestParam String search, @RequestParam int page,
                                   @RequestParam int size, Principal principal) {

        if (null == search || search.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query string cannot be empty");
        }

        if (size > maxPageSize) {
            throw new IllegalArgumentException(PAGE_REQUEST_TOO_BIG_MESSAGE.formatted(maxPageSize));
        }

        PageRequest pageObj = PageRequest.of(page, size);
        return bookRepository.searchForBooks(search, pageObj);
    }

    @GetMapping(value = {"/books", "/books/"}, params = {"genre"})
    public Page<Book> findByGenre(@RequestParam String genre, Principal principal) {
        return findByGenre(genre, 0, defaultPageSize, principal);
    }

    @GetMapping(value = {"/books", "/books/"}, params = {"genre", "page", "size"})
    public Page<Book> findByGenre(@RequestParam String genre, @RequestParam int page,
                                  @RequestParam int size, Principal principal) {

        if (null == genre || genre.trim().isEmpty()) {
            throw new IllegalArgumentException("Genre parameter cannot be empty");
        }

        if (size > maxPageSize) {
            throw new IllegalArgumentException(PAGE_REQUEST_TOO_BIG_MESSAGE.formatted(maxPageSize));
        }

        PageRequest pageObj = PageRequest.of(page, size);
        return bookRepository.findAllByGenreOrderByCreatedDateTimeDesc(pageObj, genre);
    }

    @GetMapping(value = "/books/stats")
    public SummaryStats getSummaryStats() {
        return statsService.getSummaryStats();
    }

    @GetMapping(value = "/books/genres")
    public List<BooksByGenre> findBookGenres() {
        return bookRepository.countBooksByGenre();
    }

    @GetMapping(value = "/books/authors")
    public List<BooksByAuthor> findBookAuthors() {
        return bookRepository.countBooksByAuthor();
    }

    @GetMapping(value = {"/books", "/books/"}, params = {"rating"})
    public Page<Book> findByRating(@RequestParam String rating, Principal principal) {
        return findByRating(rating, 0, defaultPageSize, principal);
    }

    @GetMapping(value = {"/books", "/books/"}, params = {"rating", "page", "size"})
    public Page<Book> findByRating(@RequestParam String rating, @RequestParam int page,
                                   @RequestParam int size, Principal principal) {

        if (null == rating || rating.trim().isEmpty()) {
            throw new IllegalArgumentException("Rating parameter cannot be empty");
        }

        Book.Rating aRating = Book.Rating.getRatingByString(rating);
        if (null == aRating) {
            throw new IllegalArgumentException("Supplied rating parameter not recognised");
        }

        if (size > maxPageSize) {
            throw new IllegalArgumentException(PAGE_REQUEST_TOO_BIG_MESSAGE.formatted(maxPageSize));
        }

        PageRequest pageObj = PageRequest.of(page, size);
        return bookRepository.findByRatingOrderByCreatedDateTimeDesc(pageObj, aRating);
    }

}
