package com.aidanwhiteley.books.repository;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.domain.googlebooks.Item;

@Repository
public class GoogleBooksDao {

	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleBooksDao.class);

	@Value("${books.google.books.api.searchUrl}")
	private String booksGoogleBooksApiSearchUrl;

	@Value("${books.google.books.api.getByIdUrl}")
	private String booksGoogleBooksApiGetByIdUrl;

	@Value("${books.google.books.api.connect.timeout}")
	private int booksGoogleBooksApiConnectTimeout;

	@Value("${books.google.books.api.read.timeout}")
	private int booksGoogleBooksApiReadTimeout;

	@Autowired
	private RestTemplate googleBooksRestTemplate;

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		builder.setConnectTimeout(booksGoogleBooksApiConnectTimeout);
		builder.setReadTimeout(booksGoogleBooksApiReadTimeout);
		return builder.build();
	}

	public BookSearchResult searchGoogBooksByTitle(String title) {

		String encodedTitle;
		try {
			encodedTitle = URLEncoder.encode(title, "UTF-8");
		} catch (UnsupportedEncodingException usee) {
			LOGGER.error("Unable to encode query string - using as is", usee);
			encodedTitle = title;
		}

		googleBooksRestTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));

		return googleBooksRestTemplate.getForObject(booksGoogleBooksApiSearchUrl + encodedTitle,
				BookSearchResult.class);
	}

	public Item searchGoogleBooksByGoogleBookId(String id) {
		googleBooksRestTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
		return googleBooksRestTemplate.getForObject(booksGoogleBooksApiGetByIdUrl + id, Item.class);
	}
}
