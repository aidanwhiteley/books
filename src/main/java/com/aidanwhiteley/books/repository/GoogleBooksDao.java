package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.googlebooks.BookSearchResult;
import com.aidanwhiteley.books.domain.googlebooks.Item;

public interface GoogleBooksDao {

    BookSearchResult searchGoogBooksByTitle(String title);
    Item searchGoogleBooksByGoogleBookId(String id);
}
