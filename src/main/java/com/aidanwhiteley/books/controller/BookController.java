package com.aidanwhiteley.books.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.GoogleBooksDao;
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
    private AuthenticationUtils authUtils;
    
    @Autowired
    private StatsService statsService;

    @Value("${books.users.default.page.size}")
    private int defaultPageSize;


    @GetMapping(value = "/books/{id}")
    public Book findBookById(@PathVariable("id") String id, Principal principal) {
        return limitDataVisibility(bookRepository.findOne(id), principal);
    }

    @GetMapping(value = "/books", params = {"author"})
    public Page<Book> findByAuthor(@RequestParam("author") String author, @RequestParam(value="page") Optional<Integer> page,
                                   @RequestParam(value="size") Optional<Integer> size, Principal principal) {

        PageRequest pageObj = new PageRequest(page.orElse(Integer.valueOf(0)).intValue(),
                size.orElse(Integer.valueOf(defaultPageSize)).intValue());
        return limitDataVisibility(bookRepository.findAllByAuthorOrderByEnteredDesc(pageObj, author), principal);
    }

    @GetMapping(value = "/books")
    public Page<Book> findAllByWhenEnteredDesc(@RequestParam(value="page") Optional<Integer> page, @RequestParam(value="size") Optional<Integer> size, Principal principal) {

        PageRequest pageObj = new PageRequest(page.orElse(Integer.valueOf(0)).intValue(),
                size.orElse(Integer.valueOf(defaultPageSize)).intValue());
        return limitDataVisibility(bookRepository.findAllByOrderByEnteredDesc(pageObj), principal);
    }

    @GetMapping(value = "/books",params = "genre")
    public Page<Book> findByGenre(@RequestParam("genre") String genre, @RequestParam(value="page") Optional<Integer> page, @RequestParam(value="size") Optional<Integer> size, Principal principal) {

        PageRequest pageObj = new PageRequest(page.orElse(Integer.valueOf(0)).intValue(),
                size.orElse(Integer.valueOf(defaultPageSize)).intValue());

        return limitDataVisibility(bookRepository.findAllByGenreOrderByEnteredDesc(pageObj, genre), principal);
    }
    
    @GetMapping(value = "/books/stats")
    public SummaryStats getSummaryStats() {
        return statsService.getSummaryStats();
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
            return limitDataVisibility(bookRepository.findByRatingOrderByEnteredDesc(pageObj, aRating), principal);
        }
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public class IllegalArgumentException extends RuntimeException {
    }

    /**
     * Remove data from Book entries if the doesnt have admin access.
     *
     * @param books
     * @param principal
     * @return
     */
    private Page<Book> limitDataVisibility(Page<Book> books, Principal principal) {

        Page<Book> filteredData = null;

        if (principal == null) {
            filteredData = books.map(Book::removeDataIfUnknownUser);
        } else {
            switch (authUtils.getUsersHighestRole(principal)) {
                case ROLE_USER:
                    filteredData = books.map(Book::removeDataIfUnknownUser);
                    break;
                case ROLE_EDITOR:
                    filteredData = books.map(Book::removeDataIfEditor);
                    break;
                case ROLE_ADMIN:
                    filteredData = books;
                    break;

                default: {
                    LOGGER.error("Unknown user roles for principal {}", principal);
                    throw new IllegalStateException("Unknown user role");
                }
            }
        }

        return filteredData;
    }

    private Book limitDataVisibility(Book book, Principal principal) {

        Book filteredData;

        if (principal == null) {
            filteredData = Book.removeDataIfUnknownUser(book);
        } else {
            switch (authUtils.getUsersHighestRole(principal)) {
                case ROLE_USER:
                    filteredData = Book.removeDataIfUnknownUser(book);
                    break;
                case ROLE_EDITOR:
                    filteredData = Book.removeDataIfEditor(book);
                    break;
                case ROLE_ADMIN:
                    filteredData = book;
                    break;

                default: {
                    LOGGER.error("Unknown user roles for principal {}", principal);
                    throw new IllegalStateException("Unknown user role");
                }
            }
        }

        return filteredData;
    }


}
