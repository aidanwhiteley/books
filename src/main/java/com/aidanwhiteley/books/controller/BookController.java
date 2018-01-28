package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.GoogleBooksDao;
import com.aidanwhiteley.books.util.AuthenticationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private GoogleBooksDao googleBooksDao;

    @Autowired
    private AuthenticationUtils authUtils;

	@RequestMapping(value = "/books/{id}", method = GET)
	public Book findBookById(@PathVariable("id") String id) {
		return bookRepository.findOne(id);
	}

	@RequestMapping(value = "/books", method = GET, params = "author")
	public List<Book> findByAuthor(@RequestParam("author") String author) {
		return bookRepository.findAllByAuthor(author);
	}

	@RequestMapping(value = "/books", method = GET)
	public Page<Book> findAllByWhenEnteredDesc(Pageable page, Principal principal) {

		return limitDataVisibility(bookRepository.findAllByOrderByEnteredDesc(page), principal);
	}

    @RequestMapping(value = "/books", method = GET, params = "genre")
	public List<Book> findByGenre(@RequestParam("genre") String genre) {
		return bookRepository.findAllByGenre(genre);
	}

	@RequestMapping(value = "/googlebooks", method = GET, params = "title")
	public BookSearchResult findGoogleBooksByTitle(@RequestParam("title") String title) {
		return googleBooksDao.searchGoogBooksByTitle(title);
	}

    /**
     * Remove data from Book entries if the user hasnt been given at least the ROLE_EDITOR level.
     *
     * @param allByOrderByEnteredDesc
     * @param principal
     * @return
     */
    private Page<Book> limitDataVisibility(Page<Book> allByOrderByEnteredDesc, Principal principal) {

	    boolean removeData = true;
	    if (principal != null) {
	        User user = authUtils.extractUserFromPrincipal(principal);
	        if (user.getRoles().contains(User.Role.ROLE_EDITOR) || user.getRoles().contains(User.Role.ROLE_ADMIN)) {
	            removeData = false;
            }
        }

        if (removeData) {
	        return allByOrderByEnteredDesc.map(Book::removeDataIfUnknownUser);
        } else {
	        return allByOrderByEnteredDesc;
        }

    }

}
