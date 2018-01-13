package com.aidanwhiteley.books.controller;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import com.aidanwhiteley.books.controller.config.WebSecurityConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.repository.BookRepositoryTest;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BookSecureControllerTest {

	private static final String GENRE_TOO_LONG = "abcdefghjijklmnopqrstuvwxyz01234567890";

	@MockBean
	private WebSecurityConfiguration noSecurityBean;

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Test
	public void createBook() {
		ResponseEntity<Book> response = postBookToServer();
		assertEquals(HttpStatus.CREATED, response.getStatusCode());
	}

	@Test
	public void tryToCreateInvalidBook() {
		// An empty book should fail
		Book emptyBook = new Book();
		HttpEntity<Book> request = new HttpEntity<>(emptyBook);
		ResponseEntity<Book> response = testRestTemplate.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

		// Create a valid book and then exceed one of the max field sizes
		Book testBook = BookRepositoryTest.createTestBook();
		testBook.setGenre(GENRE_TOO_LONG);
		request = new HttpEntity<>(testBook);
		response = testRestTemplate.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@Test
	public void updateBook() {
		ResponseEntity<Book> response = postBookToServer();
		HttpHeaders headers = response.getHeaders();
		URI uri = headers.getLocation();

		Book book = testRestTemplate.getForObject(uri, Book.class);
		final String updatedTitle = "An updated book title";
		book.setTitle(updatedTitle);
		HttpEntity<Book> putData = new HttpEntity<>(book);

		ResponseEntity<Book> putResponse = testRestTemplate.exchange("/secure/api/books", HttpMethod.PUT, putData,
				Book.class);
		assertEquals(putResponse.getStatusCode(), HttpStatus.NO_CONTENT);
		headers = response.getHeaders();
		uri = headers.getLocation();

		Book updatedBook = testRestTemplate.getForObject(uri, Book.class);
		assertEquals(updatedBook.getTitle(), updatedTitle);
	}

	private ResponseEntity<Book> postBookToServer() {
		Book testBook = BookRepositoryTest.createTestBook();
		HttpEntity<Book> request = new HttpEntity<>(testBook);

		return testRestTemplate.exchange("/secure/api/books", HttpMethod.POST, request, Book.class);
	}

}