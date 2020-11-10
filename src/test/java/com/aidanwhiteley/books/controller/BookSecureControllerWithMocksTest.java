package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.GoogleBooksDaoSync;
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class BookSecureControllerWithMocksTest {

    private static final String BOOK_ID_1 = "1234";
    private static final String GOOGLE_BOOK_ID_1 = "g1234";
    private static final String GOOGLE_BOOK_ID_2 = "g5678";
    @MockBean
    private JwtAuthenticationUtils jwtAuthenticationUtils;
    @MockBean
    private BookRepository bookRepository;
    @MockBean
    private GoogleBooksDaoSync googleBooksDaoSync;

    @Test
    public void updateBookWithoutSettingGoogleBookData() {

        Book book = Book.builder().id(BOOK_ID_1).build();
        Principal principal = initTest(book);

        BookSecureController controller = new BookSecureController(bookRepository, googleBooksDaoSync, null, jwtAuthenticationUtils);
        controller.updateBook(book, principal);

        verify(googleBooksDaoSync, times(0)).searchGoogleBooksByGoogleBookId(anyString());
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    public void updateBookSetGoogleBookDataForFirstTime() {

        Book book = Book.builder().id(BOOK_ID_1).googleBookId(GOOGLE_BOOK_ID_1).build();
        Principal principal = initTest(book);

        BookSecureController controller = new BookSecureController(bookRepository, googleBooksDaoSync, null, jwtAuthenticationUtils);
        controller.updateBook(book, principal);

        verify(googleBooksDaoSync, times(1)).searchGoogleBooksByGoogleBookId(GOOGLE_BOOK_ID_1);
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    public void updateBookSetDifferentGoogleBookData() {

        Book book1 = Book.builder().id(BOOK_ID_1).googleBookId(GOOGLE_BOOK_ID_1).build();
        Principal principal = initTest(book1);

        BookSecureController controller = new BookSecureController(bookRepository, googleBooksDaoSync, null, jwtAuthenticationUtils);
        Book book2 = Book.builder().id(BOOK_ID_1).googleBookId(GOOGLE_BOOK_ID_2).build();
        controller.updateBook(book2, principal);

        verify(googleBooksDaoSync, times(1)).searchGoogleBooksByGoogleBookId(GOOGLE_BOOK_ID_2);
        verify(bookRepository, times(1)).save(book2);
    }

    @Test
    public void updateBookSetSameGoogleBookData() {

        Book book1 = Book.builder().id(BOOK_ID_1).googleBookId(GOOGLE_BOOK_ID_1).googleBookDetails(new Item()).build();
        Principal principal = initTest(book1);

        BookSecureController controller = new BookSecureController(bookRepository, googleBooksDaoSync, null, jwtAuthenticationUtils);
        controller.updateBook(book1, principal);

        verify(googleBooksDaoSync, times(0)).searchGoogleBooksByGoogleBookId(anyString());
        verify(bookRepository, times(1)).save(book1);
    }

    private Principal initTest(Book book) {
        Principal principal = getPrincipal();
        User adminUser = getAdminUser();

        when(jwtAuthenticationUtils.extractUserFromPrincipal(principal, false))
                .thenReturn(Optional.of(adminUser));
        when(bookRepository.findById(BOOK_ID_1))
                .thenReturn(Optional.ofNullable(book));

        return principal;
    }

    private Principal getPrincipal() {
        //noinspection Convert2Lambda
        return new Principal() {
            @Override
            public String getName() {
                return null;
            }
        };
    }

    private User getAdminUser() {
        User adminUser = new User();
        adminUser.setRoles(Collections.singletonList(User.Role.ROLE_ADMIN));
        return adminUser;
    }
}
