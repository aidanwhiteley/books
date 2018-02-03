package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.GoogleBooksDao;
import com.aidanwhiteley.books.util.AuthenticationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

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

    @RequestMapping(value = "/books/{id}", method = GET)
    public Book findBookById(@PathVariable("id") String id, Principal principal) {
        return limitDataVisibility(bookRepository.findOne(id), principal);
    }

    @RequestMapping(value = "/books", method = GET, params = {"author", "page", "size"})
    public Page<Book> findByAuthor(@RequestParam("author") String author, @RequestParam("page") int page, @RequestParam("size") int size, Principal principal) {
        PageRequest pageObj = new PageRequest(page, size);
        return limitDataVisibility(bookRepository.findAllByAuthor(pageObj, author), principal);
    }

    @RequestMapping(value = "/books", params = {"page", "size"}, method = GET)
    public Page<Book> findAllByWhenEnteredDesc(@RequestParam("page") int page, @RequestParam("size") int size, Principal principal) {

        PageRequest pageObj = new PageRequest(page, size);
        return limitDataVisibility(bookRepository.findAllByOrderByEnteredDesc(pageObj), principal);
    }

    @RequestMapping(value = "/books", method = GET, params = "genre")
    public List<Book> findByGenre(@RequestParam("genre") String genre) {
        throw new UnsupportedOperationException();
    }

    @RequestMapping(value = "/googlebooks", method = GET, params = "title")
    public BookSearchResult findGoogleBooksByTitle(@RequestParam("title") String title) {
        return googleBooksDao.searchGoogBooksByTitle(title);
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
