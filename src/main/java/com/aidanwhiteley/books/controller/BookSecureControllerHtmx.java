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
import com.aidanwhiteley.books.util.FlashMessages;
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;
import com.innoq.spring.cookie.flash.CookieFlashMapManager;
import com.innoq.spring.cookie.flash.codec.jackson.JacksonFlashMapListCodec;
import com.innoq.spring.cookie.security.CookieValueSigner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.aidanwhiteley.books.util.LogDetaint.logMessageDetaint;

@PreAuthorize("hasAnyRole('ROLE_EDITOR', 'ROLE_ADMIN')")
@Controller
public class BookSecureControllerHtmx implements BookControllerHtmxExceptionHandling {

    private final BookRepository bookRepository;
    private final JwtAuthenticationUtils authUtils;
    private final StatsService statsService;
    private final GoogleBookSearchService googleBookSearchService;
    private final GoogleBooksDaoAsync googleBooksDaoAsync;
    private final FlashMessages flashMessages;

    private static final String NO_VALUE_SELECTED = "NO_VALUE_SELECTED";
    private static final Logger LOGGER = LoggerFactory.getLogger(BookSecureControllerHtmx.class);

    @Value("${books.users.default.page.size}")
    private int defaultPageSize;

    public BookSecureControllerHtmx(BookRepository bookRepository, JwtAuthenticationUtils jwtAuthenticationUtils,
                                    StatsService statsService, GoogleBookSearchService googleBookSearchService,
                                    GoogleBooksDaoAsync googleBooksDaoAsync, FlashMessages flashMessages) {
        this.bookRepository = bookRepository;
        this.authUtils = jwtAuthenticationUtils;
        this.statsService = statsService;
        this.googleBookSearchService = googleBookSearchService;
        this.googleBooksDaoAsync = googleBooksDaoAsync;
        this.flashMessages = flashMessages;
    }

    @GetMapping(value = {"/createreview"})
    public String createBookReview(Model model, Principal principal) {

        model.addAttribute("bookForm", new BookForm());
        model.addAttribute("genres", getGenres());
        model.addAttribute("index", -1);
        model.addAttribute("iscreate", true);
        model.addAttribute("isupdate", false);
        model.addAttribute("actionUrl", "/createreview");
        addUserToModel(principal, model);

        return "create-update-review";
    }

    @GetMapping(value = "/updatereview/{bookId}")
    public String updateBookReview(@PathVariable String bookId, Model model, Principal principal) {

        var book = bookRepository.findById(bookId);
        var bookForm = BookForm.getBookFormFromBook(
                book.orElseThrow(() -> new NotFoundException("Book id " + bookId + " not found")));

        // Get a list of matching books into cache - we are not using the return value here!
        googleBookSearchService.getGoogleBooks(bookForm.getTitle(), bookForm.getAuthor(), 0);

        var googleBookSearchresult = bookForm.getGoogleBookSearchResult();
        googleBookSearchresult.setHasMore(true);

        model.addAttribute("bookForm", bookForm);
        model.addAttribute("googleBookSearchResult", googleBookSearchresult);
        model.addAttribute("genres", getGenres());
        model.addAttribute("booktitle", bookForm.getTitle());
        model.addAttribute("author", bookForm.getAuthor());
        model.addAttribute("index", 0);
        model.addAttribute("iscreate", false);
        model.addAttribute("isupdate", true);
        model.addAttribute("actionUrl", "/updatereview");
        addUserToModel(principal, model);

        return "create-update-review";
    }

    @PostMapping(value = {"/createreview"})
    public String createBookReviewForm(@Valid @ModelAttribute BookForm bookForm, BindingResult bindingResult,
                                       HttpServletRequest request, HttpServletResponse response,
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
            model.addAttribute("index", -1);
            model.addAttribute("iscreate", true);
            model.addAttribute("isupdate", false);
            model.addAttribute("actionUrl", "/createreview");
            addUserToModel(principal, model);
            if (bookForm.getIndex() != -1) {
                // Ignoring the view string return value - just want the data added to the Model
                findGoogleBooksByTitleAndAuthor(bookForm.getTitle(), bookForm.getAuthor(), bookForm.getIndex(), model, principal);
            }

            return "create-update-review";
        }

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {

            Book aBook = bookRepository.insert(bookForm.getBookFromBookForm());

            // If there were Google Book details specified, call an async method to
            // go and get the full details from Google and then update the Mongo document for the book
            if (bookForm.getGoogleBookId() != null && bookForm.getGoogleBookId().length() > 0) {
                googleBooksDaoAsync.updateBookWithGoogleBookDetails(aBook, bookForm.getGoogleBookId());
            }

            flashMessages.storeFlashMessage("message", "Book review added successfully", request, response);

            return "redirect:/bookreview?bookId=" + aBook.getId();
        } else {
            LOGGER.error("Couldnt create a book as user to own book not found! Principal: {}", logMessageDetaint(principal));
            throw new NotAuthorisedException("User trying to create a book review not found in user data store!");
        }
    }

    @PostMapping(value = {"/updatereview"})
    public String updateBookReviewForm(@Valid @ModelAttribute BookForm bookForm, BindingResult bindingResult,
                                       Model model, Principal principal, HttpServletRequest request, HttpServletResponse response) {

        if (bookForm.getRating().equals(NO_VALUE_SELECTED)) {
            bindingResult.rejectValue("rating", "error.rating", "You must select your rating for the book");
        }

        if (bindingResult.hasErrors()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Following form validation errors occurred: {}", bindingResult);
            }

            model.addAttribute("bookForm", bookForm);
            model.addAttribute("googleBookSearchResult", bookForm.getGoogleBookSearchResult());
            model.addAttribute("genres", getGenres());
            model.addAttribute("booktitle", bookForm.getTitle());
            model.addAttribute("author", bookForm.getAuthor());
            model.addAttribute("index", 0);
            model.addAttribute("iscreate", false);
            model.addAttribute("isupdate", true);
            model.addAttribute("actionUrl", "/updatereview");
            addUserToModel(principal, model);

            if (bookForm.getIndex() != -1) {
                // Ignoring the view string return value - just want the data added to the Model
                findGoogleBooksByTitleAndAuthor(bookForm.getTitle(), bookForm.getAuthor(), bookForm.getIndex(), model, principal);
            }

            return "create-update-review";
        }

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {

            Book aBook;

            Book currentBookState = bookRepository.findById(bookForm.getBookId())
                    .orElseThrow(() -> new IllegalArgumentException("Didn't find book with id '" + bookForm.getBookId() + "' to update"));
            if (currentBookState.isOwner(user.get()) || user.get().getRoles().contains(User.Role.ROLE_ADMIN)) {
                Book inputBook = bookForm.getBookFromBookForm();
                Book mergedBook = BookSecureController.mergeUpdatesOntoExistingBook(currentBookState, inputBook);
                aBook = bookRepository.save(mergedBook);
            } else {
                LOGGER.error("An attempt to update book id {} was made by {} without the necessary permissions",
                        bookForm.getBookId(), user.get().getFullName());
                throw new NotAuthorisedException("User did not have the permission required to update a book review");
            }

            // If there were Google Book details specified, call an async method to
            // go and get the full details from Google and then update the Mongo document for the book
            if (bookForm.getGoogleBookId() != null && bookForm.getGoogleBookId().length() > 0) {
                googleBooksDaoAsync.updateBookWithGoogleBookDetails(aBook, bookForm.getGoogleBookId());
            }

            flashMessages.storeFlashMessage("message", "Book review updated successfully", request, response);

            return "redirect:/bookreview?bookId=" + bookForm.getBookId();
        } else {
            LOGGER.error("Couldn't update a book as user to own book not found! Principal: {}", logMessageDetaint(principal));
            throw new NotAuthorisedException("User trying to update a book review not found in user data store!");
        }
    }

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

        return "create-update-review :: cloudy-google-book-candidates";
    }



    @DeleteMapping(value = "/deletereview/{id}")
    public String deleteBookReview(@PathVariable String id, Model model, Principal principal) {

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {
            Book currentBookState = bookRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Couldn't find book id " + id + " to delete"));

            if (currentBookState.isOwner(user.get()) || user.get().getRoles().contains(User.Role.ROLE_ADMIN)) {
                bookRepository.deleteById(id);
                model.addAttribute("booktitle", currentBookState.getTitle());
                model.addAttribute("author", currentBookState.getAuthor());
                model.addAttribute("reviewer", currentBookState.getCreatedBy().getFullName());
                addUserToModel(principal, model);

                return "delete-book-confirmation :: cloudy-delete-confirmation";
            } else {
                LOGGER.warn("User {} {} tried to delete book id {} '{}' without the necessary permissions", user.get().getFullName(),
                        user.get().getId(), currentBookState.getId(), currentBookState.getTitle());
                throw new AccessDeniedException("User tried to delete book without necessary permissions");
            }
        } else {
            LOGGER.error("A user that doesnt exist in the database tried to delete book id {}", id);
            throw new NotFoundException("User not found when trying to delete a book review");
        }
    }

    @Override
    public void addUserToModel(Principal principal, Model model) {

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {
            model.addAttribute("user", user.get());
            model.addAttribute("highestRole", user.get().getHighestRole().getShortName());
        } else {
            model.addAttribute("user", null);
            model.addAttribute("highestRole", null);
        }
    }


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
        model.addAttribute("ratings", BookControllerHtmx.getRatings(""));
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

    private List<BooksByReader> getReviewers(Principal principal) {
        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent() && user.get().getHighestRole().getRoleNumber() >= User.Role.ROLE_EDITOR.getRoleNumber()) {
            return bookRepository.countBooksByReader();
        } else {
            LOGGER.warn("A Principal (user) should be present for this method to have been called");
            return new ArrayList<>();
        }
    }

    private List<BooksByGenre> getGenres() {
        return bookRepository.countBooksByGenre();
    }

    private List<BooksByAuthor> getAuthors() {
        return bookRepository.countBooksByAuthor();
    }

}
