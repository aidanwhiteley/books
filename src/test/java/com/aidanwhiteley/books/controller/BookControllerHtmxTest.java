package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.BookRepositoryTest;
import com.aidanwhiteley.books.util.IntegrationTest;
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("ConstantConditions")
@AutoConfigureMockMvc
public class BookControllerHtmxTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private MockMvc mockMvc;


    @Test
    void findBookById() throws Exception {
        String bookId = getIdForNewBook();

        var result = mockMvc.perform(get("/bookreview?bookId=" + bookId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();
        var element = Jsoup.parse(output).selectFirst("p");
        assertTrue(element.html().contains(BookRepositoryTest.J_UNIT_TESTING_FOR_BEGINNERS));
    }

    @Test
    void findByAuthor() throws Exception {
        BookControllerTestUtils.postBookToServer(jwtUtils, testRestTemplate);
        var result = mockMvc.perform(get("/find?pagenum=1&author=" + BookRepositoryTest.DR_ZEUSS))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();
        var element = Jsoup.parse(output).selectFirst("td.firstTableCol");
        assertTrue(element.html().contains(BookRepositoryTest.J_UNIT_TESTING_FOR_BEGINNERS));
    }

//    @Test
//    @WithMockUser(roles="EDITOR")
//    void testUserDataIsReturnedToEditorUser() throws Exception {
//        String bookId = getIdForNewBook();
//
//        var jwtAuthlUtilsMock = Mockito.mock(JwtAuthenticationUtils.class);
//        Mockito.when(jwtAuthlUtilsMock.extractUserFromPrincipal(any(Principal.class), eq(false)))
//                .thenReturn(Optional.of(BookControllerTestUtils.getEditorTestUser()));
//
//        var result = mockMvc.perform(get("/bookreview?bookId=" + bookId))
//                .andExpect(status().isOk())
//                .andExpect(content().contentTypeCompatibleWith("text/html"))
//                .andReturn();
//        var output = result.getResponse().getContentAsString();
//
//        var element = Jsoup.parse(output).selectFirst("span.reviewer");
//        assertTrue(element.html().contains(BookControllerTestUtils.USER_WITH_ALL_ROLES_FULL_NAME));
//    }

    @Test
    void testUserDataIsNotReturnedToBasicUser() throws Exception {
        String bookId = getIdForNewBook();

        var result = mockMvc.perform(get("/bookreview?bookId=" + bookId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();

        var element = Jsoup.parse(output).selectFirst("span.reviewer");
        assertNull(element);
    }

    private String getIdForNewBook() {
        ResponseEntity<Book> response = BookControllerTestUtils.postBookToServer(jwtUtils, testRestTemplate);
        int lastSlash = response.getHeaders().getLocation().getPath().lastIndexOf("/");
        String bookId = response.getHeaders().getLocation().getPath().substring(lastSlash + 1);
        return bookId;
    }


}