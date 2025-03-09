package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.exceptions.NotFoundException;
import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService;
import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.BookRepositoryTest;
import jakarta.servlet.http.Cookie;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0, httpsPort = 0)
public class BookSecureControllerHtmxTest {

    // We use this existing book id because it is also configured in the WireMock stubbing
    // of the calls to the Google Books API
    public static final String ASK_AN_ASTRONAUT_EXISTING_BOOK_ID = "5a8c81a754ef065d0c1cc63e";

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void testNonEditorUserCantAccessCreateReviewPage() throws Exception {

        var result = mockMvc.perform(get("/createreview"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        var output = result.getResponse().getContentAsString();
        assertEquals("e-403", Jsoup.parse(output).getElementById("errorCode").html());
    }

    @Test
    void testEditorUserCanAccessCreateReviewPage() throws Exception {

        String token = jwtUtils.createTokenForUser(BookControllerTestUtils.getEditorTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        mockMvc.perform(MockMvcRequestBuilders.get("/createreview")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
    }

    @Test
    void testEditorUserCanAccessUpdateReviewPage() throws Exception {

        String token = jwtUtils.createTokenForUser(BookControllerTestUtils.getEditorTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        mockMvc.perform(MockMvcRequestBuilders.get("/updatereview/" + ASK_AN_ASTRONAUT_EXISTING_BOOK_ID)
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
    }

    @Test
    void testEditorCanCreateBookReview() throws Exception {

        String token = jwtUtils.createTokenForUser(BookControllerTestUtils.getEditorTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        final String title = "Ask An Astronaut";
        final String author = "Tim Peake";
        MockHttpServletRequestBuilder createReview = post("/createreview")
                .cookie(cookie)
                .with(csrf())
                .param("title", title)
                .param("author", author)
                .param("genre", "Autobiography")
                .param("summary", "Some review summary text")
                .param("rating", "GREAT")
                .param("googleBookId", "m141DwAAQBAJ")
                .param("index", "0");

        var output = mockMvc.perform(createReview)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        String flashMessage = output.getResponse().getHeaderValue(BookSecureControllerHtmx.HX_TRIGGER_AFTER_SWAP).toString();
        assertTrue(flashMessage.contains("success"));

        // Get the new books id and check it has been saved correctly
        String retargetUrl = output.getResponse().getHeaderValue("hx-push-url").toString();
        String bookId = retargetUrl.split("=")[1];
        Book savedBook = bookRepository.findById(bookId).orElseThrow(() -> new NotFoundException("Book id " + bookId + " not found"));
        assertEquals(title, savedBook.getTitle());
        assertEquals(author, savedBook.getAuthor());
    }

    @Test
    void testCreateBookReviewInvalidData() throws Exception {

        String token = jwtUtils.createTokenForUser(BookControllerTestUtils.getEditorTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        final String title = "Ask An Astronaut";
        final String author = "Tim Peake";
        // Fill in some invalid data
        MockHttpServletRequestBuilder createReview = post("/createreview")
                .cookie(cookie)
                .with(csrf())
                .param("title", title)
                .param("author", author)
                .param("genre", "")
                .param("summary", "")
                .param("rating", "WRONG")
                .param("googleBookId", "m141DwAAQBAJ")
                .param("index", "0");

        var output = mockMvc.perform(createReview)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                //.andDo(print())
                .andReturn();

        var html = output.getResponse().getContentAsString();
        var elements = Jsoup.parse(html).select(".invalid-feedback");
        assertTrue(elements.size() > 0);

        String flashMessage = output.getResponse().getHeaderValue(BookSecureControllerHtmx.HX_TRIGGER_AFTER_SWAP).toString();
        assertTrue(flashMessage.contains("warn"));
    }


    @Test
    void testAdminCanUpdateBookReview() throws Exception {

        String token = jwtUtils.createTokenForUser(BookControllerTestUtils.getTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        final String title = "Ask An Astronaut";
        final String author = "Tim Peake";
        final String updatedReviewSummary = "Here is an updated review";
        MockHttpServletRequestBuilder createReview = post("/updatereview")
                .cookie(cookie)
                .with(csrf())
                .param("bookId", ASK_AN_ASTRONAUT_EXISTING_BOOK_ID)
                .param("title", title)
                .param("author", author)
                .param("genre", "Autobiography")
                .param("summary", updatedReviewSummary)
                .param("rating", "GREAT")
                .param("googleBookId", "m141DwAAQBAJ")
                .param("index", "0");

        var output = mockMvc.perform(createReview)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        String flashMessage = output.getResponse().getHeaderValue(BookSecureControllerHtmx.HX_TRIGGER_AFTER_SWAP).toString();
        assertTrue(flashMessage.contains("success"));

        Book updatedBook = bookRepository.findById(ASK_AN_ASTRONAUT_EXISTING_BOOK_ID).orElseThrow(() ->
                new NotFoundException("Book id " + ASK_AN_ASTRONAUT_EXISTING_BOOK_ID + " not found"));
        assertEquals(title, updatedBook.getTitle());
        // And now the updated summary
        assertEquals(updatedReviewSummary, updatedBook.getSummary());
    }

}