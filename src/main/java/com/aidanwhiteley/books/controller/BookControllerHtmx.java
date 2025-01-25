package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.dtos.BookForm;
import com.aidanwhiteley.books.controller.exceptions.NotAuthorisedException;
import com.aidanwhiteley.books.controller.exceptions.NotFoundException;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.GoogleBooksDaoAsync;
import com.aidanwhiteley.books.repository.dtos.BooksByAuthor;
import com.aidanwhiteley.books.repository.dtos.BooksByGenre;
import com.aidanwhiteley.books.repository.dtos.BooksByReader;
import com.aidanwhiteley.books.service.GoogleBookSearchService;
import com.aidanwhiteley.books.service.StatsService;
import com.aidanwhiteley.books.service.dtos.GoogleBookSearchResult;
import com.aidanwhiteley.books.service.dtos.SummaryStats;
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import org.springframework.data.mongodb.UncategorizedMongoDbException;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.aidanwhiteley.books.domain.Book.Rating.GREAT;
import static com.aidanwhiteley.books.util.LogDetaint.logMessageDetaint;

@Controller
public class BookControllerHtmx {

    private final BookRepository bookRepository;
    private final JwtAuthenticationUtils authUtils;
    private final StatsService statsService;
    private final GoogleBookSearchService googleBookSearchService;
    private final GoogleBooksDaoAsync googleBooksDaoAsync;

    private static final String NO_VALUE_SELECTED = "NO_VALUE_SELECTED";
    private static final Logger LOGGER = LoggerFactory.getLogger(BookControllerHtmx.class);

    @Value("${books.users.default.page.size}")
    private int defaultPageSize;

    @Value("${books.users.max.page.size}")
    private int maxPageSize;

    public BookControllerHtmx(BookRepository bookRepository, JwtAuthenticationUtils jwtAuthenticationUtils,
                              StatsService statsService, GoogleBookSearchService googleBookSearchService,
                              GoogleBooksDaoAsync googleBooksDaoAsync) {
        this.bookRepository = bookRepository;
        this.authUtils = jwtAuthenticationUtils;
        this.statsService = statsService;
        this.googleBookSearchService = googleBookSearchService;
        this.googleBooksDaoAsync = googleBooksDaoAsync;
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
            return "recently-reviewed";
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
        model.addAttribute("authors", getAuthors());
        model.addAttribute("genres", getGenres());
        model.addAttribute("reviewers", getReviewers(principal));
        addUserToModel(principal, model);

        return "find-reviews";
    }

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

        model.addAttribute("pageOfBooks", books);
        model.addAttribute("ratings", getRatings(""));
        model.addAttribute("authors", getAuthors());
        model.addAttribute("genres", getGenres());
        model.addAttribute("reviewers", getReviewers(principal));
        addUserToModel(principal, model);
        model.addAttribute("paginationLink", "find?rating=" + rating);

        if (hxRequest) {
            return "find-reviews :: cloudy-find-by-results";
        } else {
            return "find-reviews";
        }
    }

    @GetMapping(value = {"/find"}, params = {"author", "pagenum"})
    public String findByAuthor(Model model, Principal principal, @RequestParam String author, @RequestParam int pagenum,
                               @RequestHeader(value="HX-Request", required = false) boolean hxRequest) {

        if (author == null || author.trim().isEmpty()) {
            throw new IllegalArgumentException("Author parameter cannot be empty");
        }

        if (pagenum < 1) {
            throw new IllegalArgumentException("Cannot request a page less than 1");
        }

        PageRequest pageObj = PageRequest.of(pagenum - 1, defaultPageSize);
        Page<Book> books = bookRepository.findAllByAuthorOrderByCreatedDateTimeDesc(pageObj, author);
        model.addAttribute("pageOfBooks", books);
        model.addAttribute("ratings", getRatings(""));
        model.addAttribute("authors", getAuthors());
        model.addAttribute("reviewers", getReviewers(principal));
        if (hxRequest) {
            return "find-reviews :: cloudy-find-by-results";
        } else {
            return "find-reviews";
        }
    }

    @GetMapping(value = {"/find"}, params = {"genre", "pagenum"})
    public String findByGenre(Model model, Principal principal, @RequestParam String genre, @RequestParam int pagenum,
                               @RequestHeader(value="HX-Request", required = false) boolean hxRequest) {

        if (genre == null || genre.trim().isEmpty()) {
            throw new IllegalArgumentException("Genre parameter cannot be empty");
        }

        if (pagenum < 1) {
            throw new IllegalArgumentException("Cannot request a page less than 1");
        }

        PageRequest pageObj = PageRequest.of(pagenum - 1, defaultPageSize);
        Page<Book> books = bookRepository.findAllByGenreOrderByCreatedDateTimeDesc(pageObj, genre);
        model.addAttribute("pageOfBooks", books);
        model.addAttribute("ratings", getRatings(""));
        model.addAttribute("authors", getAuthors());
        model.addAttribute("genres", getGenres());
        model.addAttribute("reviewers", getReviewers(principal));
        addUserToModel(principal, model);
        model.addAttribute("paginationLink", "find?genre=" + genre);

        if (hxRequest) {
            return "find-reviews :: cloudy-find-by-results";
        } else {
            return "find-reviews";
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_EDITOR', 'ROLE_ADMIN')")
    @GetMapping(value = {"/find"}, params = {"reviewer", "pagenum"})
    public String findByReviewer(Model model, Principal principal, @RequestParam String reviewer, @RequestParam int pagenum,
                              @RequestHeader(value="HX-Request", required = false) boolean hxRequest) {

        if (reviewer == null || reviewer.trim().isEmpty()) {
            throw new IllegalArgumentException("Genre parameter cannot be empty");
        }

        if (pagenum < 1) {
            throw new IllegalArgumentException("Cannot request a page less than 1");
        }

        PageRequest pageObj = PageRequest.of(pagenum - 1, defaultPageSize);
        Page<Book> books = bookRepository.findByReaderOrderByCreatedDateTimeDesc(pageObj, reviewer);
        model.addAttribute("pageOfBooks", books);
        model.addAttribute("ratings", getRatings(""));
        model.addAttribute("authors", getAuthors());
        model.addAttribute("genres", getGenres());
        model.addAttribute("reviewers", getReviewers(principal));
        addUserToModel(principal, model);
        model.addAttribute("paginationLink", "find?reviewer=" + reviewer);

        if (hxRequest) {
            return "find-reviews :: cloudy-find-by-results";
        } else {
            return "find-reviews";
        }
    }

    @GetMapping(value = {"/search"}, params = {"term"})
    public String findBySearchFullPage(Model model, Principal principal, @RequestParam String term) {
        return findBySearch(model, principal, term, 1, false);
    }

    @GetMapping(value = {"/search"}, params = {"term", "pagenum"})
    public String findBySearch(Model model, Principal principal, @RequestParam String term, @RequestParam int pagenum,
                                   @RequestHeader(value="HX-Request", required = false) boolean hxRequest) {

        if (null == term || term.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query string cannot be empty");
        }

        if (pagenum < 1) {
            throw new IllegalArgumentException("Cannot request a page less than 1");
        }

        PageRequest pageObj = PageRequest.of(pagenum - 1    , defaultPageSize);
        Page<Book> books = bookRepository.searchForBooks(term, pageObj);

        model.addAttribute("pageOfBooks", books);
        addUserToModel(principal, model);
        model.addAttribute("paginationLink", "search?term=" + term);

        if (hxRequest) {
            return "find-reviews :: cloudy-find-by-results";
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

    @PreAuthorize("hasAnyRole('ROLE_EDITOR', 'ROLE_ADMIN')")
    @GetMapping(value = {"/createreview"})
    public String createBookReview(Model model, Principal principal) {

        model.addAttribute("bookForm", new BookForm());
        model.addAttribute("genres", getGenres());
        model.addAttribute("index", -1);
        addUserToModel(principal, model);

        return "create-review";
    }

    @PreAuthorize("hasAnyRole('ROLE_EDITOR', 'ROLE_ADMIN')")
    @PostMapping(value = {"/createreview"})
    public String createBookReviewForm(@Valid @ModelAttribute BookForm bookForm, BindingResult bindingResult,
                                       Model model, Principal principal) {

        if (bookForm.getRating().equals(NO_VALUE_SELECTED)) {
            bindingResult.rejectValue("rating", "error.rating", "You must select your rating for the book");
        }

        if (bindingResult.hasErrors()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Following form validation errors occurred: {}", bindingResult);
            }

            model.addAttribute("bookForm", bookForm);
            model.addAttribute("genres", getGenres());
            addUserToModel(principal, model);
            if (bookForm.getIndex() != -1) {
                findGoogleBooksByTitleAndAuthor(bookForm.getTitle(), bookForm.getAuthor(), bookForm.getIndex(), model, principal);
            }

            return "create-review";
        }

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {

            Book insertedBook = bookRepository.insert(bookForm.getBookFromBookForm());

            // If there were Google Book details specified, call an async method to
            // go and get the full details from Google and then update the Mongo document for the book
            if (bookForm.getGoogleBookId() != null && bookForm.getGoogleBookId().length() > 0) {
                googleBooksDaoAsync.updateBookWithGoogleBookDetails(insertedBook, bookForm.getGoogleBookId());
            }

            return recentlyReviewedByPage(1, model, principal, false);
        } else {
            LOGGER.error("Couldnt create a book as user to own book not found! Principal: {}", logMessageDetaint(principal));
            throw new NotAuthorisedException("User trying to create a book review not found in user data store!");
        }


    }

    @PreAuthorize("hasAnyRole('ROLE_EDITOR', 'ROLE_ADMIN')")
    @GetMapping(value = {"/googlebooks"}, params = {"title", "author", "index"})
    public String findGoogleBooksByTitleAndAuthor(@RequestParam String title, @RequestParam String author,
                                                  @RequestParam int index, Model model, Principal principal) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Calling Google Book Search API with title '{}', author '{}' and index '{}'",
                    title, author, index);
        }
        GoogleBookSearchResult result = googleBookSearchService.getGoogleBooks(title, author, index);

        model.addAttribute("googleBookSearchResult", result);
        model.addAttribute("booktitle", title);
        model.addAttribute("author", author);
        model.addAttribute("index", index);
        if (result.getItem() != null) {
            model.addAttribute("googleBookId", result.getItem().getId());
        } else {
            model.addAttribute("googleBookId", "");
        }
        addUserToModel(principal, model);

        return "create-review :: cloudy-google-book-candidates";
    }

    private List<Book.Rating> getRatings(String prefix) {
        List<Book.Rating> ratings = Arrays.stream(Book.Rating.values()).toList();
        if (!prefix.isEmpty()) {
            ratings = ratings.stream().filter(s -> s.name().startsWith(prefix)).toList();
        }
        return ratings.reversed();
    }

    private List<BooksByAuthor> getAuthors() {
        return bookRepository.countBooksByAuthor();
    }

    private List<BooksByGenre> getGenres() {
        return bookRepository.countBooksByGenre();
    }

    private List<BooksByReader> getReviewers(Principal principal) {
        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent() && user.get().getHighestRole().getRoleNumber() >= User.Role.ROLE_EDITOR.getRoleNumber()) {
            return bookRepository.countBooksByReader();
        } else {
            return new ArrayList<>();
        }
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
            model.addAttribute("user", null);
            model.addAttribute("highestRole", null);
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

    // The REST API part of this application registers a global @RestControllerAdvice to centrally handle exceptions
    // and they generally return JSON to the client.
    // As we want to leave the REST API in place, we handle exceptions locally in this HTMX based controller
    // so that we can return HTML views for any errors from this controller.

    @ExceptionHandler(UncategorizedMongoDbException.class)
    public String handleInMemoryMongoFulLTextSearchException(UncategorizedMongoDbException ex, Model model, Principal principal) {
        LOGGER.error("An UncategorizedMongoDbException occurred. This is normally expected when running in " +
                "development mode when trying to use the Search as full text indexes aren't supported by " +
                "the in memory fake Mongo. However, as it is just possible that it could happen for other " +
                "reasons, the full stack trace is logged", ex);
        String description = "Search doesn't work when running against the in-memory Mongo " +
                "used in development because full text indexes are not supported in that implementation.";
        return addAttributesToErrorPage(description, "mongo-uncategorized", model, principal);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException ex, Model model, Principal principal) {
        LOGGER.error("An unacceptable input was received. Either this is an application error or someone manually sending incorrect parameters", ex);
        String description = "Sorry - the values sent to the application are not acceptable.";
        return addAttributesToErrorPage(description, "400", model, principal);
    }

    @ExceptionHandler(NotFoundException.class)
    public String handleNotFoundException(NotFoundException ex, Model model, Principal principal) {
        LOGGER.error("The application couldn't find the resource requested - {}", ex.getMessage(), ex);
        String description = "Sorry - the application could not find what you wanted";
        return addAttributesToErrorPage(description, "404", model, principal);
    }

    @ExceptionHandler(NotAuthorisedException.class)
    public String handleNotAuthorisedException(NotAuthorisedException ex, Model model, Principal principal) {
        LOGGER.error("An attempt was made to access a protected resource without the required authorisation - {}", ex.getMessage(), ex);
        String description = "Sorry - you are not authorised to access this functionality";
        return addAttributesToErrorPage(description, "401", model, principal);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDeniedException(AccessDeniedException ex, Model model, Principal principal) {
        LOGGER.error("An attempt was made to access a protected resource without the required permission - {}", ex.getMessage(), ex);
        String description = "Sorry - you are not permitted to access this functionality";
        return addAttributesToErrorPage(description, "403", model, principal);
    }

    private String addAttributesToErrorPage(String description, String code, Model model, Principal principal) {
        model.addAttribute("description", "Search doesn't work when running against the in-memory Mongo " +
                "used in development because full text indexes are not supported in that implementation.");
        model.addAttribute("code", code);
        model.addAttribute("dateTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        addUserToModel(principal, model);
        return "error";
    }

}
