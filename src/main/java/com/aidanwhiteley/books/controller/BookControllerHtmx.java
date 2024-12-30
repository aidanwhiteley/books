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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.aidanwhiteley.books.domain.Book.Rating.GREAT;

@Controller
public class BookControllerHtmx {

    public static final String PAGE_REQUEST_TOO_BIG_MESSAGE = "Cannot request a page of data containing more than %s elements";

    private final BookRepository bookRepository;
    private final JwtAuthenticationUtils authUtils;

    private static final Logger LOGGER = LoggerFactory.getLogger(BookControllerHtmx.class);

    @Value("${books.users.default.page.size}")
    private int defaultPageSize;

    @Value("${books.users.max.page.size}")
    private int maxPageSize;

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
        addUserToModel(principal, model);

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
        addUserToModel(principal, model);

        return "components/swiper :: cloudy-swiper-slides";
    }

    @GetMapping(value = "/recent")
    public String recentlyReviewed(Model model, Principal principal) {
        return recentlyReviewedByPage(1, model, principal, false);
    }

    @GetMapping(value = "/recent", params = {"pagenum"})
    public String recentlyReviewedByPage(@RequestParam int pagenum, Model model, Principal principal,
                                         @RequestHeader(value="HX-Request", required = false) boolean hxRequest) {
        PageRequest pageObj = PageRequest.of(pagenum - 1, 7);
        Page<Book> page = bookRepository.findAllByOrderByCreatedDateTimeDesc(pageObj);
        model.addAttribute("pageOfBooks", page);
        addUserToModel(principal, model);
        model.addAttribute("paginationLink", "/recent");

        if (hxRequest) {
            return "find-reviews :: cloudy-find-by-results";
        } else {
            return "recently-reviewed.html";
        }
    }

    @GetMapping(value = "/bookreview", params = {"bookId"})
    public String bookReview(@RequestParam String bookId, Model model, Principal principal) {
        Book aBook = bookRepository.findById(bookId).orElseThrow(() -> new NotFoundException("Book id " + bookId + " not found"));
        model.addAttribute("book", aBook);
        addUserToModel(principal, model);

        return "book-review.html";
    }

    @GetMapping(value = "/find")
    public String findReviews(Model model, Principal principal) {
        List<Book.Rating> ratings = getRatings("");
        model.addAttribute("ratings", ratings);
        addUserToModel(principal, model);

        return "find-reviews";
    }

//    @GetMapping(value = "/findbookratings", params = {"ratingprefix"})
//    public String findBookRatings(Model model, Principal principal, @RequestParam String ratingprefix) {
//        List<Book.Rating> ratings = getRatings(ratingprefix);
//        model.addAttribute("ratings", ratings);
//        model.addAttribute("books", null);
//        return "find-reviews :: cloudy-find-by-ratings-options";
//    }

    @GetMapping(value = {"/find"}, params = {"rating", "pagenum"})
    public String findByRating(Model model, Principal principal, @RequestParam String rating, @RequestParam int pagenum,
                               @RequestHeader(value="HX-Request", required = false) boolean hxRequest) {

        if (null == rating || rating.trim().isEmpty()) {
            throw new IllegalArgumentException("Rating parameter cannot be empty");
        }

        Book.Rating aRating = Book.Rating.getRatingByString(rating);
        if (null == aRating) {
            throw new IllegalArgumentException("Supplied rating parameter not recognised");
        }

        if (pagenum < 1) {
            throw new IllegalArgumentException("Cannot request a page less than 1");
        }

        PageRequest pageObj = PageRequest.of(pagenum - 1, defaultPageSize);
        Page<Book> books = bookRepository.findByRatingOrderByCreatedDateTimeDesc(pageObj, aRating);

        List<Book.Rating> ratings = getRatings("");
        model.addAttribute("ratings", ratings);

        model.addAttribute("pageOfBooks", books);
        addUserToModel(principal, model);
        model.addAttribute("paginationLink", "find?rating=" + rating);

        if (hxRequest) {
            return "find-reviews :: cloudy-find-by-results";
        } else {
            return "find-reviews";
        }
    }

    private List<Book.Rating> getRatings(String prefix) {
        List<Book.Rating> ratings = Arrays.stream(Book.Rating.values()).toList();
        if (!prefix.isEmpty()) {
            ratings = ratings.stream().filter(s -> s.name().startsWith(prefix)).toList();
        }
        return ratings.reversed();
    }

    private void addUserToModel(Principal principal, Model model) {

        if (principal == null) {
            LOGGER.debug("Principal passed to user method was null");
            model.addAttribute("user", null);
            model.addAttribute("highestRole", null);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Principal passed in to user method is: {}", principal.toString().replaceAll("[\n\r\t]", "_"));
            }
        }

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {
            model.addAttribute("user", user.get());
            model.addAttribute("highestRole", user.get().getHighestRole().getShortName());
        } else {
            // We've been supplied a valid JWT but the user is no longer in the database.
            LOGGER.warn("No user was found for the given principal - assuming an old JWT supplied for a user removed from data store");
            model.addAttribute("user", null);
            model.addAttribute("highestRole", null);
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
