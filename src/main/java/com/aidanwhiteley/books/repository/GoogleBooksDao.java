package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

@Repository
public class GoogleBooksDao {

	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleBooksDao.class);

	@Value("${books.google.books.api.searchUrl}")
	private String booksGoogleBooksApiSearchUrl;

	@Value("${books.google.books.api.getByIdUrl}")
	private String booksGoogleBooksApiGetByIdUrl;
	
	@Value("${books.google.books.api.countryCode}")
	private String booksGoogleBooksApiCountryCode;

	@Value("${books.google.books.api.connect.timeout}")
	private int booksGoogleBooksApiConnectTimeout;

	@Value("${books.google.books.api.read.timeout}")
	private int booksGoogleBooksApiReadTimeout;

	private final RestTemplate googleBooksRestTemplate;

    @Autowired
	public GoogleBooksDao(RestTemplateBuilder restTemplateBuilder) {
	    this.googleBooksRestTemplate = buildRestTemplate(restTemplateBuilder);
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

		return googleBooksRestTemplate.getForObject(booksGoogleBooksApiSearchUrl + encodedTitle + "&" + booksGoogleBooksApiCountryCode,
				BookSearchResult.class);
	}

	public Item searchGoogleBooksByGoogleBookId(String id) {
		googleBooksRestTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
		try {
		return googleBooksRestTemplate.getForObject(booksGoogleBooksApiGetByIdUrl + id + "/?" + booksGoogleBooksApiCountryCode , Item.class);
		} catch (HttpStatusCodeException e){
			String errorpayload = e.getResponseBodyAsString();
			LOGGER.error("Error calling Google Books API: " + errorpayload, e);
			throw e;
		} catch(RestClientException e){
			LOGGER.error("Rest client error calling Google Books API: ", e);
			throw e;
		}
	}

    private RestTemplate buildRestTemplate(RestTemplateBuilder builder) {
        builder.setConnectTimeout(booksGoogleBooksApiConnectTimeout);
        builder.setReadTimeout(booksGoogleBooksApiReadTimeout);
        return builder.build();
    }
}
