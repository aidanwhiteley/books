package com.aidanwhiteley.books.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import com.aidanwhiteley.books.controller.util.LimitBookDataVisibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.GoogleBooksDao;
import com.aidanwhiteley.books.repository.dtos.BooksByAuthor;
import com.aidanwhiteley.books.repository.dtos.BooksByGenre;
import com.aidanwhiteley.books.service.StatsService;
import com.aidanwhiteley.books.service.dtos.SummaryStats;
import com.aidanwhiteley.books.util.AuthenticationUtils;

@RestController
@RequestMapping("/api")
public class BookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookController.class);

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private GoogleBooksDao googleBooksDao;
    
    @Autowired
    private StatsService statsService;

    @Autowired
    private LimitBookDataVisibility dataVisibilityService;

    @Value("${books.users.default.page.size}")
    private int defaultPageSize;


    @GetMapping(value = "/books")
    public Page<Book> findAllByWhenEnteredDesc(@RequestParam(value="page") Optional<Integer> page, @RequestParam(value="size") Optional<Integer> size, Principal principal) {

        PageRequest pageObj = new PageRequest(page.orElse(Integer.valueOf(0)).intValue(),
                size.orElse(Integer.valueOf(defaultPageSize)).intValue());
        return dataVisibilityService.limitDataVisibility(bookRepository.findAllByOrderByEnteredDesc(pageObj), principal);
    }

    @GetMapping(value = "/books/{id}")
    public Book findBookById(@PathVariable("id") String id, Principal principal) {
        return dataVisibilityService.limitDataVisibility(bookRepository.findOne(id), principal);
    }


    @GetMapping(value = "/books", params = {"author"})
    public Page<Book> findByAuthor(@RequestParam("author") String author, @RequestParam(value="page") Optional<Integer> page,
                                   @RequestParam(value="size") Optional<Integer> size, Principal principal) {

        PageRequest pageObj = new PageRequest(page.orElse(Integer.valueOf(0)).intValue(),
                size.orElse(Integer.valueOf(defaultPageSize)).intValue());
        return dataVisibilityService.limitDataVisibility(bookRepository.findAllByAuthorOrderByEnteredDesc(pageObj, author), principal);
    }


    @GetMapping(value = "/books",params = "genre")
    public Page<Book> findByGenre(@RequestParam("genre") String genre, @RequestParam(value="page") Optional<Integer> page, @RequestParam(value="size") Optional<Integer> size, Principal principal) {

        PageRequest pageObj = new PageRequest(page.orElse(Integer.valueOf(0)).intValue(),
                size.orElse(Integer.valueOf(defaultPageSize)).intValue());

        return dataVisibilityService.limitDataVisibility(bookRepository.findAllByGenreOrderByEnteredDesc(pageObj, genre), principal);
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
    public Page<Book> findByRating(@RequestParam("rating") String rating, @RequestParam(value="page") Optional<Integer> page, @RequestParam(value="size") Optional<Integer> size, Principal principal) {

        Book.Rating aRating = Book.Rating.getRatingByString(rating);
        PageRequest pageObj = new PageRequest(page.orElse(Integer.valueOf(0)).intValue(),
                size.orElse(Integer.valueOf(defaultPageSize)).intValue());

        if (aRating == null) {
            throw new IllegalArgumentException();
        } else {
            return dataVisibilityService.limitDataVisibility(bookRepository.findByRatingOrderByEnteredDesc(pageObj, aRating), principal);
        }
    }


    @SuppressWarnings("serial")
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public class IllegalArgumentException extends RuntimeException {
    }
}
