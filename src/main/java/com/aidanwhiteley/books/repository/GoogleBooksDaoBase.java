package com.aidanwhiteley.books.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public abstract class GoogleBooksDaoBase {

    static final String UTF_8 = "UTF-8";

    @Value("${books.google.books.api.searchUrl}")
    protected String booksGoogleBooksApiSearchUrl;

    @Value("${books.google.books.api.getByIdUrl}")
    protected String booksGoogleBooksApiGetByIdUrl;

    @Value("${books.google.books.api.countryCode}")
    protected String booksGoogleBooksApiCountryCode;

    @Value("${books.google.books.api.connect.timeout}")
    protected int booksGoogleBooksApiConnectTimeout;

    @Value("${books.google.books.api.read.timeout}")
    protected int booksGoogleBooksApiReadTimeout;

    public void setBooksGoogleBooksApiSearchUrl(String booksGoogleBooksApiSearchUrl) {
        this.booksGoogleBooksApiSearchUrl = booksGoogleBooksApiSearchUrl;
    }

    public void setBooksGoogleBooksApiGetByIdUrl(String booksGoogleBooksApiGetByIdUrl) {
        this.booksGoogleBooksApiGetByIdUrl = booksGoogleBooksApiGetByIdUrl;
    }

    public void setBooksGoogleBooksApiCountryCode(String booksGoogleBooksApiCountryCode) {
        this.booksGoogleBooksApiCountryCode = booksGoogleBooksApiCountryCode;
    }

    public void setBooksGoogleBooksApiConnectTimeout(int booksGoogleBooksApiConnectTimeout) {
        this.booksGoogleBooksApiConnectTimeout = booksGoogleBooksApiConnectTimeout;
    }

    public void setBooksGoogleBooksApiReadTimeout(int booksGoogleBooksApiReadTimeout) {
        this.booksGoogleBooksApiReadTimeout = booksGoogleBooksApiReadTimeout;
    }
}
