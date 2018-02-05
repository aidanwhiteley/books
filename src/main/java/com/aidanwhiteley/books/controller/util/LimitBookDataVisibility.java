package com.aidanwhiteley.books.controller.util;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.util.AuthenticationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class LimitBookDataVisibility {

    private static final Logger LOGGER = LoggerFactory.getLogger(LimitBookDataVisibility.class);

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

    public Book limitDataVisibility(Book book, Principal principal) {

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
