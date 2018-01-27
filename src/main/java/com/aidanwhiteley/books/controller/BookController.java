package com.aidanwhiteley.books.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.GoogleBooksDao;

@RestController
@RequestMapping("/api")
public class BookController {

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private GoogleBooksDao googleBooksDao;

	@RequestMapping(value = "/books/{id}", method = GET)
	public Book findBookById(@PathVariable("id") String id) {
		return bookRepository.findOne(id);
	}

	@RequestMapping(value = "/books", method = GET, params = "author")
	public List<Book> findByAuthor(@RequestParam("author") String author) {
		return bookRepository.findAllByAuthor(author);
	}

	@RequestMapping(value = "/books", method = GET)
	public Page<Book> findAllByLastRead(Pageable page) {
		return bookRepository.findAllByOrderByEnteredDesc(page);
	}

	@RequestMapping(value = "/books", method = GET, params = "genre")
	public List<Book> findByGenre(@RequestParam("genre") String genre) {
		return bookRepository.findAllByGenre(genre);
	}

	@RequestMapping(value = "/googlebooks", method = GET, params = "title")
	public BookSearchResult findGoogleBooksByTitle(@RequestParam("title") String title) {
		return googleBooksDao.searchGoogBooksByTitle(title);
	}

}
