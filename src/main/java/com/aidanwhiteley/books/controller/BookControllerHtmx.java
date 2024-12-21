package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.exceptions.NotAuthorisedException;
import com.aidanwhiteley.books.controller.exceptions.NotFoundException;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static com.aidanwhiteley.books.domain.Book.Rating.GREAT;

@Controller
public class BookControllerHtmx {

    private final BookRepository bookRepository;
    private final JwtAuthenticationUtils authUtils;

    private static final Logger LOGGER = LoggerFactory.getLogger(BookControllerHtmx.class);

    public BookControllerHtmx(BookRepository bookRepository, JwtAuthenticationUtils jwtAuthenticationUtils) {
        this.bookRepository = bookRepository;
        this.authUtils = jwtAuthenticationUtils;
    }

    @GetMapping(value = "/")
    public String index(Model model, Principal principal) {
        PageRequest pageObj = PageRequest.of(0, 30);
        Page<Book> page = bookRepository.findByRatingOrderByCreatedDateTimeDesc(pageObj, GREAT);

        List<Book> books = getBooksWithRequiredImages(page);
        model.addAttribute("books", books.stream().toList());
        model.addAttribute("rating", "great");
        model.addAttribute("user", getUser(principal));

        return "home";
    }

    @GetMapping(value = {"/getBooksByRating"}, params = {"rating", "bookRating"})
    public String findByRating(Model model, @RequestParam String rating, Principal principal) {

        Book.Rating ipRating = Book.Rating.getRatingByString(rating);
        if (ipRating == null) {
            throw new IllegalArgumentException("Input rating " + rating + " not valid");
        }

        PageRequest pageObj = PageRequest.of(0, 30);
        Page<Book> page = bookRepository.findByRatingOrderByCreatedDateTimeDesc(pageObj, ipRating);

        List<Book> books = getBooksWithRequiredImages(page);
        model.addAttribute("books", books.stream().toList());
        model.addAttribute("rating", rating);
        model.addAttribute("user", getUser(principal));

        return "components/swiper :: cloudy-swiper";
    }

    @GetMapping(value = "/recent")
    public String recentlyReviewed(Model model, Principal principal) {
        return recentlyReviewedByPage(1, model, principal);
    }

    @GetMapping(value = "/recent", params = {"pagenum"})
    public String recentlyReviewedByPage(@RequestParam int pagenum, Model model, Principal principal) {
        PageRequest pageObj = PageRequest.of(pagenum - 1, 7);
        Page<Book> page = bookRepository.findAllByOrderByCreatedDateTimeDesc(pageObj);
        model.addAttribute("pageOfBooks", page);
        model.addAttribute("user", getUser(principal));

        return "recently-reviewed.html";
    }

    @GetMapping(value = "/bookreview", params = {"bookId"})
    public String bookReview(@RequestParam String bookId, Model model, Principal principal) {
        Book aBook = bookRepository.findById(bookId).orElseThrow(() -> new NotFoundException("Book id " + bookId + " not found"));
        model.addAttribute("book", aBook);
        model.addAttribute("user", getUser(principal));

        return "book-review.html";
    }

    private User getUser(Principal principal) {

        if (principal == null) {
            LOGGER.debug("Principal passed to user method was null");
            return null;
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Principal passed in to user method is: {}", principal.toString().replaceAll("[\n\r\t]", "_"));
            }
        }

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {
            return user.get();
        } else {
            // We've been supplied a valid JWT but the user is no longer in the database.
            LOGGER.warn("No user was found for the given principal - assuming an old JWT supplied for a user removed from data store");
            throw new NotAuthorisedException("No user found in user store for input JWT- please clear your cookies");
        }
    }

    private static List<Book> getBooksWithRequiredImages(Page<Book> page) {
        return page.getContent().stream().filter(b ->
                        (b.getGoogleBookId() != null &&
                                !b.getGoogleBookId().isBlank() &&
                                (b.getGoogleBookDetails().getVolumeInfo().getImageLinks().getThumbnail() != null) &&
                                !b.getGoogleBookDetails().getVolumeInfo().getImageLinks().getThumbnail().isBlank()
                        )).toList();
    }
}
