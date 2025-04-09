package com.aidanwhiteley.books.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService;
import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.util.BookTestUtils;
import de.bwaldvogel.mongo.wire.MongoWireProtocolHandler;
import jakarta.servlet.http.Cookie;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev-mongo-java-server")
public class BookControllerHtmxTest {

    public static final String EXISTING_BOOK_ID = "5a8c81a754ef065d0c1cc63e";

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private Environment environment;

    @Test
    void findBooksOnHomePage() throws Exception {

        var result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();
        var elements = Jsoup.parse(output).select(".swiper-slide");
        assertTrue(elements.size() >= 10);
    }

    @Test
    void findBookById() throws Exception {
        String bookId = createTestBook().getId();

        var result = mockMvc.perform(get("/bookreview?bookId=" + bookId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();
        var element = Jsoup.parse(output).selectFirst("p");
        assertTrue(element.html().contains(BookTestUtils.J_UNIT_TESTING_FOR_BEGINNERS));
    }

    @Test
    void findByAuthor() throws Exception {
        createTestBook();
        var result = mockMvc.perform(get("/find?pagenum=1&author=" + BookTestUtils.DR_ZEUSS)
                        .header(BookControllerHtmx.HX_REQUEST, true))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();
        var element = Jsoup.parse(output).selectFirst("td.firstTableCol");
        assertTrue(element.html().contains(BookTestUtils.J_UNIT_TESTING_FOR_BEGINNERS));
        assertTrue(result.getResponse().containsHeader(BookControllerHtmx.HX_TRIGGER_AFTER_SWAP));
    }

    @Test
    void findByAuthorBadParams() throws Exception {
        // Temporarily turn off unwanted logging during this specific test
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("OFF"));

        createTestBook();
        var result = mockMvc.perform(get("/find?pagenum=0&author=" + BookTestUtils.DR_ZEUSS))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();
        assertEquals("e-400", Jsoup.parse(output).getElementById("errorCode").html());

        mockMvc.perform(get("/find?pagenum=1&author="))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("ON"));
    }

    @Test
    void findByRating() throws Exception {
        var result = mockMvc.perform(get("/find?pagenum=1&rating=great")
                        .header(BookControllerHtmx.HX_REQUEST, true))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();
        var elements = Jsoup.parse(output).select("tr .firstTableCol");
        assertFalse(elements.isEmpty());
        assertTrue(result.getResponse().containsHeader(BookControllerHtmx.HX_TRIGGER_AFTER_SWAP));
    }

    @Test
    void findByRatingBadParams() throws Exception {
        // Don't want test logs filled with expected exceptions. If the test fails, comment this out to debug!
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("OFF"));

        mockMvc.perform(get("/find?pagenum=1&rating="))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        mockMvc.perform(get("/find?pagenum=1&rating=BAD-DATA"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        mockMvc.perform(get("/find?pagenum=0&rating=great"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("ON"));
    }

    @Test
    void findByGenre() throws Exception {
        var result = mockMvc.perform(get("/find?pagenum=1&genre=Novel")
                        .header(BookControllerHtmx.HX_REQUEST, true))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();
        var elements = Jsoup.parse(output).select("tr .firstTableCol");
        assertTrue(elements.size() >= 6);
        assertTrue(result.getResponse().containsHeader(BookControllerHtmx.HX_TRIGGER_AFTER_SWAP));
    }

    @Test
    void findByGenreBadParams() throws Exception {
        // Don't want test logs filled with expected exceptions. If the test fails, comment this out to debug!
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("OFF"));

        mockMvc.perform(get("/find?pagenum=1&genre="))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        mockMvc.perform(get("/find?pagenum=0&genre=Novel"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("ON"));
    }

    @Test
    void searchBadParams() throws Exception {
        // Don't want test logs filled with expected exceptions. If the test fails, comment this out to debug!
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("OFF"));

        mockMvc.perform(get("/search?term="))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        mockMvc.perform(get("/search?term=TEST&pagenum=0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("ON"));
    }

    @Test
    void findRecent() throws Exception {
        var result = mockMvc.perform(get("/recent?pagenum=1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();
        var elements = Jsoup.parse(output).select("tr .firstTableCol");
        assertTrue(elements.size() >= 8);
    }

    @Test
    void getBookByRating() throws Exception {
        var result = mockMvc.perform(get("/getBooksByRating?rating=great")
                        .header(BookControllerHtmx.HX_REQUEST, true))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();
        var elements = Jsoup.parse(output).select(".swiper-slide");

        assertTrue(elements.size() >= 10);
        assertTrue(result.getResponse().containsHeader(BookControllerHtmx.HX_TRIGGER_AFTER_SWAP));
    }

    @Test
    void getBookByRatingMissingParam() throws Exception {
        // Don't want test logs filled with expected exceptions. If the test fails, comment this out to debug!
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("OFF"));

        mockMvc.perform(get("/getBooksByRating?rating=BAD-DATA"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("ON"));
    }

    @Test
    void findOptions() throws Exception {
        var result = mockMvc.perform(get("/find"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();
        var html = Jsoup.parse(output);
        assertFalse(html.select("#select-by-author-options option").isEmpty());
        assertFalse(html.select("#select-by-genre-options option").isEmpty());
        assertFalse(html.select("#select-by-rating-options option").isEmpty());
    }

    @Test
    void statistics() throws Exception {
        var result = mockMvc.perform(get("/statistics"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();
        assertTrue(Integer.parseInt(Jsoup.parse(output).getElementById("booksTotalCount").html()) > 0);
    }

    @Test
    void findBySearch() throws Exception {
        // mongo-java-server doesnt support full text indexes across fields
        if (Arrays.stream(this.environment.getActiveProfiles()).anyMatch(s ->
                s.contains(BookControllerTest.IN_MEMORY_MONGODB_SPRING_PROFILE))) {

            // Temporarily turn off unwanted logging during this specific test
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("OFF"));
            context.getLogger(MongoWireProtocolHandler.class).setLevel(Level.valueOf("OFF"));

            var result = mockMvc.perform(get("/search?pagenum=1&term=book"))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith("text/html"))
                    .andReturn();
            var output = result.getResponse().getContentAsString();
            assertEquals("e-mongo-full-text-search", Jsoup.parse(output).getElementById("errorCode").html());
            context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("WARN"));
            context.getLogger(MongoWireProtocolHandler.class).setLevel(Level.valueOf("WARN"));
        } else {
            var result = mockMvc.perform(get("/search?pagenum=1&term=book"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("text/html"))
                    .andReturn();
            var output = result.getResponse().getContentAsString();
            var elements = Jsoup.parse(output).select("tr .firstTableCol");
            assertFalse(elements.isEmpty());
        }
    }


    @Test
    void testUserDataIsReturnedToEditorUser() throws Exception {
        String bookId = createTestBook().getId();

        String token = jwtUtils.createTokenForUser(BookTestUtils.getEditorTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        var result = mockMvc.perform(MockMvcRequestBuilders.get("/bookreview?bookId=" + bookId)
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();

        var element = Jsoup.parse(output).selectFirst("span.reviewer");
        assertTrue(element.html().contains(BookTestUtils.USER_WITH_ALL_ROLES_FULL_NAME));
    }

    @Test
    void testUserDataIsNotReturnedToBasicUser() throws Exception {
        String bookId = createTestBook().getId();

        var result = mockMvc.perform(get("/bookreview?bookId=" + bookId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();

        var element = Jsoup.parse(output).selectFirst("span.reviewer");
        assertNull(element);
    }

    @Test
    void testAdminCanDeleteSomeoneElsesReview() throws Exception {

        String token = jwtUtils.createTokenForUser(BookTestUtils.getTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        var result = mockMvc.perform(MockMvcRequestBuilders.get("/bookreview?bookId=" + EXISTING_BOOK_ID)
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();

        assertNotNull(Jsoup.parse(output).getElementById("deleteButton"));
    }

    @Test
    void testEditorCantDeleteSomeoneElsesReview() throws Exception {

        String token = jwtUtils.createTokenForUser(BookTestUtils.getEditorTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        var result = mockMvc.perform(MockMvcRequestBuilders.get("/bookreview?bookId=" + EXISTING_BOOK_ID)
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();

        assertNull(Jsoup.parse(output).getElementById("deleteButton"));
    }

    private Book createTestBook() {
        return bookRepository.insert(BookTestUtils.createTestBook());
    }

}