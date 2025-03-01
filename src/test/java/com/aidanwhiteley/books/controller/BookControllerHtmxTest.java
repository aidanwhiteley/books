package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.BookRepositoryTest;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("ConstantConditions")
@AutoConfigureMockMvc
public class BookControllerHtmxTest extends IntegrationTest {

    public static final String IN_MEMORY_MONGODB_SPRING_PROFILE = "mongo-java-server";
    private static final String NO_AUTH_SPRING_PROFILE = "no-auth";
    private static final Logger LOGGER = LoggerFactory.getLogger(BookControllerHtmxTest.class);
    private static final String ERROR_MESSAGE_FOR_INVALID_RATING = "Supplied rating parameter not recognised";

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

    @Test
    void testUserDataIsReturnedToEditorUser() throws Exception {
        String bookId = getIdForNewBook();

        var result = mockMvc.perform(get("/bookreview?bookId=" + bookId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();

        var element = Jsoup.parse(output).selectFirst("span.reviewer");
        assertTrue(element.html().contains(BookControllerTestUtils.USER_WITH_ALL_ROLES_FULL_NAME));
    }

    private String getIdForNewBook() {
        ResponseEntity<Book> response = BookControllerTestUtils.postBookToServer(jwtUtils, testRestTemplate);
        int lastSlash = response.getHeaders().getLocation().getPath().lastIndexOf("/");
        String bookId = response.getHeaders().getLocation().getPath().substring(lastSlash + 1);
        return bookId;
    }


}