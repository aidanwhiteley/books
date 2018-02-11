package com.aidanwhiteley.books.controller.util;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.util.AuthenticationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class LimitBookDataVisibility {

    @Autowired
    private AuthenticationUtils authUtils;

    /**
     * Remove data from a Page of Book entries according to the users level of access.
     *
     * @param books
     * @param principal
     * @return The filtered Page of Books
     */
    public Page<Book> limitDataVisibility(Page<Book> books, Principal principal) {

        final User user = authUtils.extractUserFromPrincipal(principal);
        books.forEach(b -> b.setPermissionsAndContentForUser(user));

        return books;
    }

    public Book limitDataVisibility(Book book, Principal principal) {

        final User user = authUtils.extractUserFromPrincipal(principal);
        book.setPermissionsAndContentForUser(user);

        return book;
    }
}
