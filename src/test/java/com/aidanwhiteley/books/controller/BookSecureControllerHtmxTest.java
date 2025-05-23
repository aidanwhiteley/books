package com.aidanwhiteley.books.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.aidanwhiteley.books.controller.exceptions.NotFoundException;
import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService;
import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.repository.BookRepository;
import jakarta.servlet.http.Cookie;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.aidanwhiteley.books.util.BookTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0, httpsPort = 0)
@ActiveProfiles("dev-mongo-java-server")
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

        String token = jwtUtils.createTokenForUser(getEditorTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        mockMvc.perform(MockMvcRequestBuilders.get("/createreview")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
    }

    @Test
    void testInvalidJwtToken() throws Exception {
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, "INVALID_TOKEN");
        mockMvc.perform(MockMvcRequestBuilders.get("/createreview")
                        .cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
    }

    @Test
    void testEditorUserCanAccessUpdateReviewPage() throws Exception {

        String token = jwtUtils.createTokenForUser(getEditorTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        mockMvc.perform(MockMvcRequestBuilders.get("/updatereview/" + ASK_AN_ASTRONAUT_EXISTING_BOOK_ID)
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
    }

    @Test
    void testUpdateNonExistentBookReview() throws Exception {

        // Don't want test logs filled with expected exceptions. If the test fails, comment this out to debug!
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("OFF"));

        String token = jwtUtils.createTokenForUser(getEditorTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        mockMvc.perform(MockMvcRequestBuilders.get("/updatereview/DOESNT-EXIST")
                        .cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("ON"));
    }

    @Test
    void testEditorCanCreateBookReview() throws Exception {

        String token = jwtUtils.createTokenForUser(getEditorTestUser());
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
    void testEditorCannotUpdateAnotherEditorsBookReview() throws Exception {

        // Don't want test logs filled with expected exceptions. If the test fails, comment this out to debug!
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("OFF"));

        bookRepository.insert(createTestBook());

        String token = jwtUtils.createTokenForUser(getADifferentEditorTestUser());
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

        mockMvc.perform(createReview)
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("ON"));
    }

    @Test
    void testCreateBookReviewInvalidData() throws Exception {

        String token = jwtUtils.createTokenForUser(getEditorTestUser());
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
        assertFalse(elements.isEmpty());

        String flashMessage = output.getResponse().getHeaderValue(BookSecureControllerHtmx.HX_TRIGGER_AFTER_SWAP).toString();
        assertTrue(flashMessage.contains("warn"));
    }


    @Test
    void testAdminCanUpdateBookReview() throws Exception {

        String token = jwtUtils.createTokenForUser(getTestUser());
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
        // And now check the updated summary
        assertEquals(updatedReviewSummary, updatedBook.getSummary());
    }

    @Test
    void testAdminUpdateBookReviewWithBadData() throws Exception {

        String token = jwtUtils.createTokenForUser(getTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        final String title = "Ask An Astronaut";
        final String author = "Tim Peake";
        final String emptySummaryData = "";
        MockHttpServletRequestBuilder createReview = post("/updatereview")
                .cookie(cookie)
                .with(csrf())
                .param("bookId", ASK_AN_ASTRONAUT_EXISTING_BOOK_ID)
                .param("title", title)
                .param("author", author)
                .param("genre", "Autobiography")
                .param("summary", emptySummaryData)
                .param("rating", "GREAT")
                .param("googleBookId", "m141DwAAQBAJ")
                .param("index", "0");

        var output = mockMvc.perform(createReview)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        var html = output.getResponse().getContentAsString();
        var elements = Jsoup.parse(html).select(".invalid-feedback");
        assertEquals(1, elements.size());

        String flashMessage = output.getResponse().getHeaderValue(BookSecureControllerHtmx.HX_TRIGGER_AFTER_SWAP).toString();
        assertTrue(flashMessage.contains("warn"));
    }

    @Test
    void testAdminCanDeleteBookReview() throws Exception {

        Book aBook = bookRepository.insert(createTestBook());
        String bookId = aBook.getId();
        assertTrue(bookRepository.findById(bookId).isPresent());

        String token = jwtUtils.createTokenForUser(getTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        MockHttpServletRequestBuilder deleteReview = delete("/deletereview/" + aBook.getId())
                .cookie(cookie)
                .with(csrf());

        var output = mockMvc.perform(deleteReview)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        String flashMessage = output.getResponse().getHeaderValue(BookSecureControllerHtmx.HX_TRIGGER_AFTER_SWAP).toString();
        assertTrue(flashMessage.contains("deleted"));
        assertFalse(bookRepository.findById(bookId).isPresent());
    }

    @Test
    void testEditorCannotDeleteBookReviewTheyDidntCreate() throws Exception {

        // This book is created with an admin owner rather than the editor user used later on in this test
        Book aBook = bookRepository.insert(createTestBook());
        String bookId = aBook.getId();
        assertTrue(bookRepository.findById(bookId).isPresent());

        String token = jwtUtils.createTokenForUser(getEditorTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        MockHttpServletRequestBuilder deleteReview = delete("/deletereview/" + aBook.getId())
                .cookie(cookie)
                .with(csrf());

        mockMvc.perform(deleteReview)
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        assertTrue(bookRepository.findById(bookId).isPresent());
    }

    @Test
    void testEditorCanAddCommentToAnyBook() throws Exception {

        // This book is created with an admin owner rather than the editor user used later on in this test
        Book aBook = bookRepository.insert(createTestBook());
        String bookId = aBook.getId();
        assertTrue(bookRepository.findById(bookId).isPresent());

        String token = jwtUtils.createTokenForUser(getTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        final String commentText = "Here is a test comment";
        MockHttpServletRequestBuilder createComment = post("/addcomment")
                .cookie(cookie)
                .with(csrf())
                .param("comment", commentText)
                .param("bookId", bookId);

        var output = mockMvc.perform(createComment)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        String flashMessage = output.getResponse().getHeaderValue(BookSecureControllerHtmx.HX_TRIGGER_AFTER_SWAP).toString();
        assertTrue(flashMessage.contains("created"));

        Book bookWithComment = bookRepository.findById(bookId).get();
        assertEquals(1, bookWithComment.getComments().size());
    }

    @Test
    void testAddCommentBadData() throws Exception {

        // This book is created with an admin owner rather than the editor user used later on in this test
        Book aBook = bookRepository.insert(createTestBook());
        String bookId = aBook.getId();
        assertTrue(bookRepository.findById(bookId).isPresent());

        String token = jwtUtils.createTokenForUser(getTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        final String commentText = "TooSmall";
        MockHttpServletRequestBuilder createComment = post("/addcomment")
                .cookie(cookie)
                .with(csrf())
                .param("comment", commentText)
                .param("bookId", bookId);

        var output = mockMvc.perform(createComment)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        var html = output.getResponse().getContentAsString();
        var elements = Jsoup.parse(html).select(".invalid-feedback");
        assertEquals(1, elements.size());

        String flashMessage = output.getResponse().getHeaderValue(BookSecureControllerHtmx.HX_TRIGGER_AFTER_SWAP).toString();
        assertTrue(flashMessage.contains("correct"));

        Book bookWithComment = bookRepository.findById(bookId).get();
        assertEquals(0, bookWithComment.getComments().size());
    }

    @Test
    void testAddAndDeleteComment() throws Exception {

        // This book is created with an admin owner rather than the editor user used later on in this test
        Book aBook = bookRepository.insert(createTestBook());
        String bookId = aBook.getId();
        assertTrue(bookRepository.findById(bookId).isPresent());

        String token = jwtUtils.createTokenForUser(getTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        final String commentText = "Here is a test comment";
        MockHttpServletRequestBuilder createComment = post("/addcomment")
                .cookie(cookie)
                .with(csrf())
                .param("comment", commentText)
                .param("bookId", bookId);

        mockMvc.perform(createComment)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        Book bookWithComment = bookRepository.findById(bookId).get();
        String commentId = bookWithComment.getComments().getFirst().getId();

        MockHttpServletRequestBuilder deleteComment = delete("/deletecomment/?bookId=" + bookId +
                "&commentId=" + commentId)
                .cookie(cookie)
                .with(csrf());

        mockMvc.perform(deleteComment)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        Book bookWithoutComment = bookRepository.findById(bookId).get();
        assertTrue(bookWithoutComment.getComments().getFirst().isDeleted());

        deleteComment = delete("/deletecomment/?bookId=" + "" +
                "&commentId=" + "")
                .cookie(cookie)
                .with(csrf());

        // Temporarily turn off unwanted logging during this specific test
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("OFF"));

        mockMvc.perform(deleteComment)
                .andExpect(status().isBadRequest());

        deleteComment = delete("/deletecomment/?bookId=" +  bookId +
                "&commentId=" + "")
                .cookie(cookie)
                .with(csrf());
        mockMvc.perform(deleteComment)
                .andExpect(status().isBadRequest());

        context.getLogger(BookControllerHtmxExceptionHandling.class).setLevel(Level.valueOf("ON"));

    }

    @Test
    void testEditorCanSeeBookReviewers() throws Exception {

        String token = jwtUtils.createTokenForUser(getEditorTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        final String reviewer = "Fred Bloggs";

        var result = mockMvc.perform(MockMvcRequestBuilders.get("/find?reviewer=" + reviewer + "&pagenum=1")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        var output = result.getResponse().getContentAsString();
        var html = Jsoup.parse(output);
        assertFalse(html.select("td.firstTableCol").isEmpty());
    }

    @Test
    void testEditorCannotSeeUserAdmin() throws Exception {

        String token = jwtUtils.createTokenForUser(getEditorTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        MockHttpServletRequestBuilder deleteReview = get("/useradmin")
                .cookie(cookie)
                .with(csrf());
        mockMvc.perform(deleteReview)
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
    }

    @Test
    void testAdminCanSeeUserAdmin() throws Exception {

        String token = jwtUtils.createTokenForUser(getTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        MockHttpServletRequestBuilder deleteReview = get("/useradmin")
                .cookie(cookie)
                .with(csrf());
        mockMvc.perform(deleteReview)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
    }

    @Test
    void testAdminCannotDeleteOwnUserid() throws Exception {

        final String idOfCurrentTestUser = "5a6cc95fba03402460427b4a";

        String token = jwtUtils.createTokenForUser(getTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        MockHttpServletRequestBuilder deleteUser = delete("/deleteuser/" + idOfCurrentTestUser)
                .cookie(cookie)
                .with(csrf());
        var output = mockMvc.perform(deleteUser)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        String flashMessage = output.getResponse().getHeaderValue(BookSecureControllerHtmx.HX_TRIGGER_AFTER_SWAP).toString();
        assertTrue(flashMessage.contains("cannot delete"));
    }

    @Test
    void testAdminCanDeleteOtherUserid() throws Exception {

        final String idOfAnotherTestUser = "5a6cc95fba03402460427b4b";

        String token = jwtUtils.createTokenForUser(getTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        MockHttpServletRequestBuilder deleteUser = delete("/deleteuser/" + idOfAnotherTestUser)
                .cookie(cookie)
                .with(csrf());
        var output = mockMvc.perform(deleteUser)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        String flashMessage = output.getResponse().getHeaderValue(BookSecureControllerHtmx.HX_TRIGGER_AFTER_SWAP).toString();
        assertTrue(flashMessage.contains("successfully deleted"));
    }

    @Test
    void testAdminCannotChangeOwnRoles() throws Exception {

        final String idOfCurrentTestUser = "5a6cc95fba03402460427b4a";

        String token = jwtUtils.createTokenForUser(getTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        MockHttpServletRequestBuilder updateUser = put("/updateuserrole/" + idOfCurrentTestUser + "?role=ANYTHING")
                .cookie(cookie)
                .with(csrf());
        var output = mockMvc.perform(updateUser)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        String flashMessage = output.getResponse().getHeaderValue(BookSecureControllerHtmx.HX_TRIGGER_AFTER_SWAP).toString();
        assertTrue(flashMessage.contains("cannot change the role"));
    }

    @Test
    void testAdminCanChangeRoleOfOtherUserid() throws Exception {

        final String idOfAnotherTestUser = "5a6ccababa03401ce44d8381";

        String token = jwtUtils.createTokenForUser(getTestUser());
        Cookie cookie = new Cookie(JwtAuthenticationService.JWT_COOKIE_NAME, token);

        MockHttpServletRequestBuilder updateUser = put("/updateuserrole/" + idOfAnotherTestUser + "?role=ADMIN")
                .cookie(cookie)
                .with(csrf());
        var output = mockMvc.perform(updateUser)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        String flashMessage = output.getResponse().getHeaderValue(BookSecureControllerHtmx.HX_TRIGGER_AFTER_SWAP).toString();
        assertTrue(flashMessage.contains("role updated OK"));
    }

}