package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.dtos.CommentForm;
import com.aidanwhiteley.books.controller.exceptions.NotFoundException;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.dtos.BooksByAuthor;
import com.aidanwhiteley.books.repository.dtos.BooksByGenre;
import com.aidanwhiteley.books.repository.dtos.BooksByReader;
import com.aidanwhiteley.books.service.StatsService;
import com.aidanwhiteley.books.service.dtos.SummaryStats;
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.aidanwhiteley.books.domain.Book.Rating.GREAT;

@Controller
public class BookControllerHtmx implements BookControllerHtmxExceptionHandling {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookControllerHtmx.class);
    public static final String HX_TRIGGER_AFTER_SWAP = "HX-Trigger-After-Swap";
    public static final String PAGE_OF_BOOKS = "pageOfBooks";
    public static final String PAGINATION_LINK = "paginationLink";
    public static final String FIND_REVIEWS_CLOUDY_FIND_BY_RESULTS = "find-reviews :: cloudy-find-by-results";
    public static final String RATINGS = "ratings";
    public static final String AUTHORS = "authors";
    public static final String GENRES = "genres";
    public static final String REVIEWERS = "reviewers";
    public static final String FIND_REVIEWS = "find-reviews";
    public static final String HIGHEST_ROLE = "highestRole";
    public static final String IS_OWNER = "isOwner";
    public static final String COMMENT_FORM = "commentForm";
    public static final String HX_REQUEST = "HX-Request";
    private final BookRepository bookRepository;
    private final JwtAuthenticationUtils authUtils;
    private final StatsService statsService;
    @Value("${books.users.default.page.size}")
    private int defaultPageSize;

    public BookControllerHtmx(BookRepository bookRepository, JwtAuthenticationUtils jwtAuthenticationUtils,
                              StatsService statsService) {
        this.bookRepository = bookRepository;
        this.authUtils = jwtAuthenticationUtils;
        this.statsService = statsService;
    }

    protected static List<Book.Rating> getRatings(String prefix) {
        List<Book.Rating> ratings = Arrays.stream(Book.Rating.values()).toList();
        if (!prefix.isEmpty()) {
            ratings = ratings.stream().filter(s -> s.name().startsWith(prefix)).toList();
        }
        return ratings.reversed();
    }

    @GetMapping(value = "/")
    public String index(Model model, Principal principal, HttpServletResponse response,
                        @RequestHeader(value = HX_REQUEST, required = false) boolean hxRequest) {
        PageRequest pageObj = PageRequest.of(0, 30);
        Page<Book> page = bookRepository.findByRatingOrderByCreatedDateTimeDesc(pageObj, GREAT);

        List<Book> books = getBooksWithRequiredImages(page);
        model.addAttribute("books", books.stream().toList());
        model.addAttribute("rating", "great");
        addUserToModel(principal, model);

        if (hxRequest) {
            response.addHeader(HX_TRIGGER_AFTER_SWAP, "initSwiper");
        }

        return "home";
    }

    @GetMapping(value = {"/getBooksByRating"}, params = {"rating"})
    public String findByRating(Model model, @RequestParam String rating, Principal principal,
                               HttpServletResponse response,
                               @RequestHeader(value = HX_REQUEST, required = false) boolean hxRequest) {

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

        if (hxRequest) {
            response.addHeader(HX_TRIGGER_AFTER_SWAP, "initSwiper");
        }

        return "components/swiper :: cloudy-swiper-slides";
    }

    @GetMapping(value = "/recent")
    public String recentlyReviewed(Model model, Principal principal) {
        return recentlyReviewedByPage(1, model, principal, false);
    }

    @GetMapping(value = "/recent", params = {"pagenum"})
    public String recentlyReviewedByPage(@RequestParam int pagenum, Model model, Principal principal,
                                         @RequestHeader(value = HX_REQUEST, required = false) boolean hxRequest) {
        PageRequest pageObj = PageRequest.of(pagenum - 1, 7);
        Page<Book> page = bookRepository.findAllByOrderByCreatedDateTimeDesc(pageObj);

        model.addAttribute(PAGE_OF_BOOKS, page);
        addUserToModel(principal, model);
        model.addAttribute(PAGINATION_LINK, "/recent");

        if (hxRequest) {
            return FIND_REVIEWS_CLOUDY_FIND_BY_RESULTS;
        } else {
            return "recently-reviewed";
        }
    }

    @GetMapping(value = "/bookreview", params = {"bookId"})
    public String bookReview(@RequestParam String bookId, Model model, Principal principal, HttpServletRequest request) {
        Book aBook = bookRepository.findById(bookId).orElseThrow(() -> new NotFoundException("Book id " + bookId + " not found"));

        model.addAttribute("book", aBook);
        model.addAttribute(COMMENT_FORM, new CommentForm());
        addIsOwnerToModel(aBook, principal, model);
        addUserToModel(principal, model);

        return "book-review";
    }

    @GetMapping(value = "/find")
    public String findReviews(Model model, Principal principal) {
        List<Book.Rating> ratings = getRatings("");
        model.addAttribute(RATINGS, ratings);
        model.addAttribute(AUTHORS, getAuthors());
        model.addAttribute(GENRES, getGenres());
        model.addAttribute(REVIEWERS, getReviewers(principal));
        addUserToModel(principal, model);

        return FIND_REVIEWS;
    }

    @GetMapping(value = {"/find"}, params = {"rating", "pagenum"})
    public String findByRating(Model model, Principal principal, @RequestParam String rating, @RequestParam int pagenum,
                               HttpServletResponse response,
                               @RequestHeader(value = HX_REQUEST, required = false) boolean hxRequest) {

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

        model.addAttribute(PAGE_OF_BOOKS, books);
        model.addAttribute(RATINGS, getRatings(""));
        model.addAttribute(AUTHORS, getAuthors());
        model.addAttribute(GENRES, getGenres());
        model.addAttribute(REVIEWERS, getReviewers(principal));
        addUserToModel(principal, model);
        model.addAttribute(PAGINATION_LINK, "find?rating=" + rating);

        if (hxRequest) {
            response.addHeader(HX_TRIGGER_AFTER_SWAP, "clearSelectRating");
            return FIND_REVIEWS_CLOUDY_FIND_BY_RESULTS;
        } else {
            return FIND_REVIEWS;
        }
    }

    @GetMapping(value = {"/find"}, params = {"author", "pagenum"})
    public String findByAuthor(Model model, Principal principal, @RequestParam String author, @RequestParam int pagenum,
                               HttpServletResponse response,
                               @RequestHeader(value = HX_REQUEST, required = false) boolean hxRequest) {

        if (author == null || author.trim().isEmpty()) {
            throw new IllegalArgumentException("Author parameter cannot be empty");
        }

        if (pagenum < 1) {
            throw new IllegalArgumentException("Cannot request a page less than 1");
        }

        PageRequest pageObj = PageRequest.of(pagenum - 1, defaultPageSize);
        Page<Book> books = bookRepository.findAllByAuthorOrderByCreatedDateTimeDesc(pageObj, author);
        model.addAttribute(PAGE_OF_BOOKS, books);
        model.addAttribute(RATINGS, getRatings(""));
        model.addAttribute(AUTHORS, getAuthors());
        model.addAttribute(REVIEWERS, getReviewers(principal));
        addUserToModel(principal, model);
        model.addAttribute(PAGINATION_LINK, "find?author=" + author);
        if (hxRequest) {
            response.addHeader(HX_TRIGGER_AFTER_SWAP, "clearSelectAuthor");
            return FIND_REVIEWS_CLOUDY_FIND_BY_RESULTS;
        } else {
            return FIND_REVIEWS;
        }
    }

    @GetMapping(value = {"/find"}, params = {"genre", "pagenum"})
    public String findByGenre(Model model, Principal principal, @RequestParam String genre, @RequestParam int pagenum,
                              HttpServletResponse response,
                              @RequestHeader(value = HX_REQUEST, required = false) boolean hxRequest) {

        if (genre == null || genre.trim().isEmpty()) {
            throw new IllegalArgumentException("Genre parameter cannot be empty");
        }

        if (pagenum < 1) {
            throw new IllegalArgumentException("Cannot request a page less than 1");
        }

        PageRequest pageObj = PageRequest.of(pagenum - 1, defaultPageSize);
        Page<Book> books = bookRepository.findAllByGenreOrderByCreatedDateTimeDesc(pageObj, genre);

        model.addAttribute(PAGE_OF_BOOKS, books);
        model.addAttribute(RATINGS, getRatings(""));
        model.addAttribute(AUTHORS, getAuthors());
        model.addAttribute(GENRES, getGenres());
        model.addAttribute(REVIEWERS, getReviewers(principal));
        addUserToModel(principal, model);
        model.addAttribute(PAGINATION_LINK, "find?genre=" + genre);

        if (hxRequest) {
            response.addHeader(HX_TRIGGER_AFTER_SWAP, "clearSelectGenre");
            return FIND_REVIEWS_CLOUDY_FIND_BY_RESULTS;
        } else {
            return FIND_REVIEWS;
        }
    }

    @GetMapping(value = {"/search"}, params = {"term"})
    public String findBySearchFullPage(Model model, Principal principal, @RequestParam String term) {
        return findBySearch(model, principal, term, 1, false);
    }

    @GetMapping(value = {"/search"}, params = {"term", "pagenum"})
    public String findBySearch(Model model, Principal principal, @RequestParam String term, @RequestParam int pagenum,
                               @RequestHeader(value = HX_REQUEST, required = false) boolean hxRequest) {

        if (null == term || term.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query string cannot be empty");
        }

        if (pagenum < 1) {
            throw new IllegalArgumentException("Cannot request a page less than 1");
        }

        PageRequest pageObj = PageRequest.of(pagenum - 1, defaultPageSize);
        Page<Book> books = bookRepository.searchForBooks(term, pageObj);

        model.addAttribute(PAGE_OF_BOOKS, books);
        addUserToModel(principal, model);
        model.addAttribute(PAGINATION_LINK, "search?term=" + term);

        if (hxRequest) {
            return FIND_REVIEWS_CLOUDY_FIND_BY_RESULTS;
        } else {
            return "search-books";
        }
    }

    @GetMapping(value = {"/statistics"})
    public String statistics(Model model, Principal principal) {

        SummaryStats stats = statsService.getSummaryStats();
        model.addAttribute("countOfBooks", stats.getCount());
        model.addAttribute("bookByRating", stats.getBooksByRating());
        List<BooksByGenre> booksByGenre = stats.getBookByGenre();
        model.addAttribute("bookByGenre", booksByGenre.subList(0, Math.min(booksByGenre.size(), 10)));
        addUserToModel(principal, model);

        return "book-stats";
    }

    @GetMapping(value = {"/log-on"})
    public String logon(Model model, Principal principal) {
        addUserToModel(principal, model);
        return "logon";
    }

    @GetMapping(value = "/tandcs")
    public String helpAndTandCs(Model model, Principal principal) {
        addUserToModel(principal, model);
        return "helpandtandcs";
    }

    @GetMapping(value = "/privacy")
    public String privacy(Model model, Principal principal) {
        addUserToModel(principal, model);
        return "privacy";
    }

    @Override
    public void addUserToModel(Principal principal, Model model) {

        if (principal == null) {
            LOGGER.debug("Principal passed to user method was null");
            model.addAttribute("user", null);
            model.addAttribute(HIGHEST_ROLE, null);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Principal passed in to user method is: {}", principal.toString().replaceAll("[\n\r\t]", "_"));
            }
        }

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, true);
        if (user.isPresent()) {
            model.addAttribute("user", user.get());
            model.addAttribute(HIGHEST_ROLE, user.get().getHighestRole().getShortName());
        } else {
            model.addAttribute("user", null);
            model.addAttribute(HIGHEST_ROLE, null);
        }
    }

    private void addIsOwnerToModel(Book aBook, Principal principal, Model model) {
        if (principal == null) {
            LOGGER.debug("Principal passed to addIsOwnerToModel method was null");
            model.addAttribute( IS_OWNER, false);
        } else {
            Optional<User> user = authUtils.extractUserFromPrincipal(principal, true);
            boolean isOwner = user.isPresent() && aBook.isOwner(user.get());
            model.addAttribute(IS_OWNER, isOwner);
        }
    }


    private List<BooksByAuthor> getAuthors() {
        return bookRepository.countBooksByAuthor();
    }

    private List<BooksByGenre> getGenres() {
        return bookRepository.countBooksByGenre();
    }

    private List<Book> getBooksWithRequiredImages(Page<Book> page) {
        return page.getContent().stream().filter(b ->
                (b.getGoogleBookId() != null &&
                        !b.getGoogleBookId().isBlank() &&
                        !(b.getGoogleBookDetails().getVolumeInfo().getImageLinks() == null) &&
                        (b.getGoogleBookDetails().getVolumeInfo().getImageLinks().getThumbnail() != null) &&
                        !b.getGoogleBookDetails().getVolumeInfo().getImageLinks().getThumbnail().isBlank()
                )).toList();
    }

    private List<BooksByReader> getReviewers(Principal principal) {
        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent() && user.get().getHighestRole().getRoleNumber() >= User.Role.ROLE_EDITOR.getRoleNumber()) {
            return bookRepository.countBooksByReader();
        } else {
            return new ArrayList<>();
        }
    }


}
