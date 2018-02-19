package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.aspect.LimitDataVisibility;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.GoogleBooksDao;
import com.aidanwhiteley.books.repository.dtos.BooksByAuthor;
import com.aidanwhiteley.books.repository.dtos.BooksByGenre;
import com.aidanwhiteley.books.service.StatsService;
import com.aidanwhiteley.books.service.dtos.SummaryStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@LimitDataVisibility
@RestController
@RequestMapping("/api")
public class BookController {

    private final BookRepository bookRepository;

    private final GoogleBooksDao googleBooksDao;

    private final StatsService statsService;

    @Value("${books.users.default.page.size}")
    private int defaultPageSize;


    @Autowired
    public BookController(BookRepository bookRepository, GoogleBooksDao googleBooksDao, StatsService statsService) {
        this.bookRepository = bookRepository;
        this.googleBooksDao = googleBooksDao;
        this.statsService = statsService;
    }

    @GetMapping(value = "/books")
    public Page<Book> findAllByWhenEnteredDesc(Principal principal) {
        return findAllByWhenEnteredDesc(0, defaultPageSize, principal);
    }

    @GetMapping(value = "/books", params = {"page", "size"})
    public Page<Book> findAllByWhenEnteredDesc(@RequestParam(value = "page") int page, @RequestParam(value = "size") int size, Principal principal) {

        PageRequest pageObj = new PageRequest(page, size);
        return bookRepository.findAllByOrderByEnteredDesc(pageObj);
    }

    @GetMapping(value = "/books/{id}")
    public Book findBookById(@PathVariable("id") String id, Principal principal) {
        return bookRepository.findOne(id);
    }


    @GetMapping(value = "/books", params = {"author"})
    public Page<Book> findByAuthor(@RequestParam("author") String author, Principal principal) {
        return findByAuthor(author, 0, defaultPageSize, principal);
    }

    @GetMapping(value = "/books", params = {"author", "page", "size"})
    public Page<Book> findByAuthor(@RequestParam("author") String author, @RequestParam(value = "page") int page,
                                   @RequestParam(value = "size") int size, Principal principal) {

        if (null == author || author.trim().isEmpty()) {
            throw new IllegalArgumentException("Author parameter cannot be empty");
        }
        PageRequest pageObj = new PageRequest(page, size);
        return bookRepository.findAllByAuthorOrderByEnteredDesc(pageObj, author);
    }


    @GetMapping(value = "/books", params = {"genre"})
    public Page<Book> findByGenre(@RequestParam("genre") String genre, Principal principal) {
        return findByGenre(genre, 0, defaultPageSize, principal);
    }

    @GetMapping(value = "/books", params = {"genre", "page", "size"})
    public Page<Book> findByGenre(@RequestParam("genre") String genre, @RequestParam(value = "page") int page, @RequestParam(value = "size") int size, Principal principal) {

        if (null == genre || genre.trim().isEmpty()) {
            throw new IllegalArgumentException("Genre parameter cannot be empty");
        }
        PageRequest pageObj = new PageRequest(page, size);
        return bookRepository.findAllByGenreOrderByEnteredDesc(pageObj, genre);
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


    @GetMapping(value = "/googlebooks", params = "title")
    public BookSearchResult findGoogleBooksByTitle(@RequestParam("title") String title) {
        return googleBooksDao.searchGoogBooksByTitle(title);
    }

    @GetMapping(value = "/books", params = {"rating"})
    public Page<Book> findByRating(@RequestParam("rating") String rating, Principal principal) {
        return findByRating(rating, 0, defaultPageSize, principal);
    }

    @GetMapping(value = "/books", params = {"rating", "page", "size"})
    public Page<Book> findByRating(@RequestParam("rating") String rating, @RequestParam(value = "page") int page, @RequestParam(value = "size") int size, Principal principal) {

        if (null == rating || rating.trim().isEmpty()) {
            throw new IllegalArgumentException("Rating parameter cannot be empty");
        }

        Book.Rating aRating = Book.Rating.getRatingByString(rating);
        if (null == aRating) {
            throw new IllegalArgumentException("Supplied rating parameter not recognised");
        }

        PageRequest pageObj = new PageRequest(page, size);
        return bookRepository.findByRatingOrderByEnteredDesc(pageObj, aRating);
    }


    @SuppressWarnings("serial")
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    class IllegalArgumentException extends RuntimeException {
        public IllegalArgumentException(String msg) {
            super(msg);
        }
    }
}
