package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.dtos.BookForm;
import com.aidanwhiteley.books.controller.dtos.ClientRoles;
import com.aidanwhiteley.books.controller.dtos.CommentForm;
import com.aidanwhiteley.books.controller.exceptions.NotAuthorisedException;
import com.aidanwhiteley.books.controller.exceptions.NotFoundException;
import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.Comment;
import com.aidanwhiteley.books.domain.Owner;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.repository.dtos.BooksByAuthor;
import com.aidanwhiteley.books.repository.dtos.BooksByGenre;
import com.aidanwhiteley.books.repository.dtos.BooksByReader;
import com.aidanwhiteley.books.service.GoogleBookSearchService;
import com.aidanwhiteley.books.service.dtos.GoogleBookSearchResult;
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;
import com.aidanwhiteley.books.util.LogDetaint;
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

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.aidanwhiteley.books.util.LogDetaint.logMessageDetaint;

@PreAuthorize("hasAnyRole('ROLE_EDITOR', 'ROLE_ADMIN')")
@Controller
public class BookSecureControllerHtmx implements BookControllerHtmxExceptionHandling {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookSecureControllerHtmx.class);
    public static final String HX_TRIGGER_AFTER_SWAP = "HX-Trigger-After-Swap";
    public static final String HX_RETARGET = "HX-Retarget";
    private static final String NO_VALUE_SELECTED = "NO_VALUE_SELECTED";
    public static final String BOOK_FORM = "bookForm";
    public static final String GENRES = "genres";
    public static final String GOOGLE_BOOK_SEARCH_RESULT = "googleBookSearchResult";
    public static final String BOOKTITLE = "booktitle";
    public static final String AUTHOR = "author";
    public static final String INDEX = "index";
    public static final String ISCREATE = "iscreate";
    public static final String ISUPDATE = "isupdate";
    public static final String ACTION_URL = "actionUrl";
    public static final String USERS = "users";
    public static final String HX_PUSH_URL = "HX-Push-Url";
    public static final String USERS_TABLE = "#users-table";
    public static final String USER_ADMIN_CLOUDY_USER_ADMIN_TABLE = "user-admin :: cloudy-user-admin-table";

    private final BookRepository bookRepository;
    private final JwtAuthenticationUtils authUtils;
    private final GoogleBookSearchService googleBookSearchService;
    private final BookControllerHtmx bookControllerHtmx;
    private final UserRepository userRepository;
    @Value("${books.users.default.page.size}")
    private int defaultPageSize;

    public BookSecureControllerHtmx(BookRepository bookRepository, JwtAuthenticationUtils jwtAuthenticationUtils,
                                    GoogleBookSearchService googleBookSearchService,
                                    BookControllerHtmx bookControllerHtmx,
                                    UserRepository userRepository) {
        this.bookRepository = bookRepository;
        this.authUtils = jwtAuthenticationUtils;
        this.googleBookSearchService = googleBookSearchService;
        this.bookControllerHtmx = bookControllerHtmx;
        this.userRepository = userRepository;
    }

    @GetMapping(value = {"/createreview"})
    public String createBookReview(Model model, Principal principal) {

        model.addAttribute(BOOK_FORM, new BookForm());
        model.addAttribute(GENRES, getGenres());
        model.addAttribute(INDEX, -1);
        model.addAttribute(ISCREATE, true);
        model.addAttribute(ISUPDATE, false);
        model.addAttribute(ACTION_URL, "/createreview");
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

        model.addAttribute(BOOK_FORM, bookForm);
        model.addAttribute(GOOGLE_BOOK_SEARCH_RESULT, googleBookSearchresult);
        model.addAttribute(GENRES, getGenres());
        model.addAttribute(BOOKTITLE, bookForm.getTitle());
        model.addAttribute(AUTHOR, bookForm.getAuthor());
        model.addAttribute(INDEX, 0);
        model.addAttribute(ISCREATE, false);
        model.addAttribute(ISUPDATE, true);
        model.addAttribute(ACTION_URL, "/updatereview");
        addUserToModel(principal, model);

        return "create-update-review";
    }

    @PostMapping(value = {"/createreview"})
    public String createBookReviewForm(@Valid @ModelAttribute BookForm bookForm, BindingResult bindingResult,
                                       HttpServletResponse response, Model model, Principal principal) {

        if (bookForm.getRating().equals(NO_VALUE_SELECTED)) {
            bindingResult.rejectValue("rating", "error.rating", "You must select your rating for the book");
        }

        if (bindingResult.hasErrors()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Following create form validation errors occurred: {}", bindingResult);
            }

            model.addAttribute(BOOK_FORM, bookForm);
            model.addAttribute(GENRES, getGenres());
            model.addAttribute(INDEX, -1);
            model.addAttribute(ISCREATE, true);
            model.addAttribute(ISUPDATE, false);
            model.addAttribute(ACTION_URL, "/createreview");
            addUserToModel(principal, model);
            if (bookForm.getIndex() != -1) {
                // Ignoring the view string return value - just want the data added to the Model
                findGoogleBooksByTitleAndAuthor(bookForm.getTitle(), bookForm.getAuthor(), bookForm.getIndex(), model, principal);
            }
            response.addHeader(HX_TRIGGER_AFTER_SWAP, "{ \"showFlashMessage\": {\"level\": \"warn\", \"message\": \"Please check and correct the highlighted form fields\"}}");

            return "create-update-review :: cloudy-book-review-form";
        }

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {

            Book aBook = bookRepository.insert(bookForm.getBookFromBookForm());

            // If there were Google Book details specified, go and get the full details from Google (or the local cache)
            // and then update the Mongo document for the book
            if (bookForm.getGoogleBookId() != null && !bookForm.getGoogleBookId().isEmpty()) {
                googleBookSearchService.updateBookWithGoogleBookDetails(aBook, bookForm.getTitle(), bookForm.getAuthor(), bookForm.getIndex());
            }

            model.addAttribute("book", aBook);
            model.addAttribute("commentForm", new CommentForm());
            addUserToModel(principal, model);
            response.addHeader(HX_TRIGGER_AFTER_SWAP, "{ \"showFlashMessage\": {\"level\": \"info\", \"message\": \"Your book review was " +
                    "successfully created\"}}");
            response.setHeader(HX_PUSH_URL, "/bookreview?bookId=" + aBook.getId());
            return "book-review :: cloudy-book-review";
        } else {
            LOGGER.error("Couldn't create a book as user to own book not found! Principal: {}", logMessageDetaint(principal));
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
                LOGGER.debug("Following update form validation errors occurred: {}", bindingResult);
            }

            model.addAttribute(BOOK_FORM, bookForm);
            model.addAttribute(GOOGLE_BOOK_SEARCH_RESULT, bookForm.getGoogleBookSearchResult());
            model.addAttribute(GENRES, getGenres());
            model.addAttribute(BOOKTITLE, bookForm.getTitle());
            model.addAttribute(AUTHOR, bookForm.getAuthor());
            model.addAttribute(INDEX, 0);
            model.addAttribute(ISCREATE, false);
            model.addAttribute(ISUPDATE, true);
            model.addAttribute(ACTION_URL, "/updatereview");
            addUserToModel(principal, model);

            if (bookForm.getIndex() != -1) {
                // Ignoring the view string return value - just want the data added to the Model
                findGoogleBooksByTitleAndAuthor(bookForm.getTitle(), bookForm.getAuthor(), bookForm.getIndex(), model, principal);
            }
            response.addHeader(HX_TRIGGER_AFTER_SWAP, "{ \"showFlashMessage\": {\"level\": \"warn\", \"message\": \"Please check and correct the highlighted form fields\"}}");

            return "create-update-review :: cloudy-book-review-form";
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

            // If there were Google Book details specified, go and get the full details from Google (or the local cache)
            // and then update the Mongo document for the book
            if (bookForm.getGoogleBookId() != null && !bookForm.getGoogleBookId().isEmpty()) {
                googleBookSearchService.updateBookWithGoogleBookDetails(aBook, bookForm.getTitle(), bookForm.getAuthor(), bookForm.getIndex());
            }

            model.addAttribute("book", aBook);
            model.addAttribute("commentForm", new CommentForm());
            addUserToModel(principal, model);
            response.addHeader(HX_TRIGGER_AFTER_SWAP, "{ \"showFlashMessage\": {\"level\": \"info\", \"message\": \"The book review was " +
                    "successfully updated\"}}");
            response.setHeader(HX_PUSH_URL, "/bookreview?bookId=" + bookForm.getBookId());
            return "book-review :: cloudy-book-review";
        } else {
            LOGGER.error("Couldn't update a book as user to own book not found! Principal: {}", logMessageDetaint(principal));
            throw new NotAuthorisedException("User trying to update a book review not found in user data store!");
        }
    }

    @GetMapping(value = {"/googlebooks"}, params = {"title", AUTHOR, INDEX})
    public String findGoogleBooksByTitleAndAuthor(@RequestParam String title, @RequestParam String author,
                                                  @RequestParam int index, Model model, Principal principal) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Calling Google Book Search API with title '{}', author '{}' and index '{}'",
                    LogDetaint.logMessageDetaint(title),
                    LogDetaint.logMessageDetaint(author),
                    LogDetaint.logMessageDetaint(index));
        }
        GoogleBookSearchResult result = googleBookSearchService.getGoogleBooks(title, author, index);

        model.addAttribute(GOOGLE_BOOK_SEARCH_RESULT, result);
        model.addAttribute(BOOKTITLE, title);
        model.addAttribute(AUTHOR, author);
        model.addAttribute(INDEX, index);
        if (result.getItem() != null) {
            model.addAttribute("googleBookId", result.getItem().getId());
        } else {
            model.addAttribute("googleBookId", "");
        }
        addUserToModel(principal, model);

        return "create-update-review :: cloudy-google-book-candidates";
    }


    @DeleteMapping(value = "/deletereview/{id}")
    public String deleteBookReview(@PathVariable String id, Model model, Principal principal,
                                   HttpServletResponse response) {

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {
            Book currentBookState = bookRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Couldn't find book id " + id + " to delete"));

            if (currentBookState.isOwner(user.get()) || user.get().getRoles().contains(User.Role.ROLE_ADMIN)) {
                bookRepository.deleteById(id);

                // This call is to populate the model variable - we don't use the return string
                bookControllerHtmx.recentlyReviewed(model, principal);

                response.addHeader(HX_TRIGGER_AFTER_SWAP, "{ \"showFlashMessage\": {\"level\": \"info\", \"message\": \"The review of '" +
                        currentBookState.getTitle() + "' by " + currentBookState.getAuthor() +
                        " was successfully deleted\"}}");

                response.addHeader(HX_PUSH_URL, "recent");
                return "recently-reviewed :: cloudy-recently-reviewed";
            } else {
                LOGGER.warn("User {} {} tried to delete book id {} '{}' without the necessary permissions", user.get().getFullName(),
                        user.get().getId(), currentBookState.getId(), currentBookState.getTitle());
                throw new AccessDeniedException("User tried to delete book without necessary permissions");
            }
        } else {
            LOGGER.error("A user that doesn't exist in the database tried to delete book id {}",
                    LogDetaint.logMessageDetaint(id));
            throw new NotFoundException("User not found when trying to delete a book review");
        }
    }

    @PostMapping(value = "/addcomment")
    public String addCommentToBook(@Valid @ModelAttribute CommentForm commentForm,
                                   BindingResult bindingResult, Model model, Principal principal,
                                   HttpServletResponse response) {

        if (bindingResult.hasErrors()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Following comment creation validation errors occurred: {}", bindingResult);
            }
            Book theBook = bookRepository.findById(commentForm.getBookId())
                    .orElseThrow(() -> new IllegalArgumentException("Couldn't find book id " + commentForm.getBookId() +
                            " for new comment error message"));

            model.addAttribute("commentForm", commentForm);
            model.addAttribute("book", theBook);
            addUserToModel(principal, model);
            response.addHeader(HX_TRIGGER_AFTER_SWAP, "{ \"showFlashMessage\": {\"level\": \"warn\", \"message\": \"Please check and correct your comment\"}}");
            return "book-review :: cloudy-book-comment-form";
        }

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {
            Comment comment = new Comment(commentForm.getComment(), new Owner(user.get()));
            Book updatedBook = bookRepository.addCommentToBook(commentForm.getBookId(), comment);

            model.addAttribute("commentForm", new CommentForm());
            model.addAttribute("book", updatedBook);
            addUserToModel(principal, model);
            response.addHeader(HX_TRIGGER_AFTER_SWAP, "{ \"showFlashMessage\": {\"level\": \"info\", \"message\": \"Your comment on the book review was " +
                    "successfully created\"}}");
            return "book-review :: cloudy-book-comment-form";
        } else {
            LOGGER.error("A user that doesnt exist in the database tried to create a book review comment");
            throw new NotFoundException("User not found when trying to create a book review comment");
        }
    }

    @DeleteMapping(value = "/deletecomment/", params = {"bookId", "commentId"})
    public String deleteCommentFromBook(@RequestParam String bookId, @RequestParam String commentId,
                                      Principal principal, Model model, HttpServletResponse response) {

        if (bookId == null || bookId.isBlank() || commentId == null || commentId.isBlank()) {
            throw new IllegalArgumentException("Missing parameters when trying to delete a book review comment");
        }

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);

        if (user.isPresent()) {
            Book currentBook = bookRepository.findById(bookId)
                    .orElseThrow(() -> new IllegalArgumentException("Unable to find book to delete comment from"));
            Comment comment = currentBook.getComments().stream().filter(c -> c.getId().equals(commentId)).findFirst()
                    .orElse(null);
            if (comment == null) {
                throw new IllegalArgumentException("Unknown commentId supplied to deleteCommentFromBook");
            }

            if (comment.isOwner(user.get()) || user.get().getRoles().contains(User.Role.ROLE_ADMIN)) {
                Book book = bookRepository.removeCommentFromBook(bookId, commentId, user.get().getFullName());
                model.addAttribute("book", book);
                addUserToModel(principal, model);
                response.addHeader(HX_TRIGGER_AFTER_SWAP, "{ \"showFlashMessage\": {\"level\": \"info\", \"message\": \"Your comment on the '" + currentBook.getTitle() +
                                "' book review was successfully deleted\"}}");
                return "book-review :: cloudy-book-comments-list";

            } else {
                throw new NotAuthorisedException("Attempt to delete a comment but the user is not owner of comment or admin");
            }
        } else {
            throw new NotAuthorisedException("Attempt to delete a comment but the user not found!");
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
                                 HttpServletResponse response,
                                 @RequestHeader(value = "HX-Request", required = false) boolean hxRequest) {

        if (reviewer == null || reviewer.trim().isEmpty()) {
            throw new IllegalArgumentException("Reviewer parameter cannot be empty");
        }

        if (pagenum < 1) {
            throw new IllegalArgumentException("Cannot request a page less than 1");
        }

        PageRequest pageObj = PageRequest.of(pagenum - 1, defaultPageSize);
        Page<Book> books = bookRepository.findByReaderOrderByCreatedDateTimeDesc(pageObj, reviewer);
        model.addAttribute("pageOfBooks", books);
        model.addAttribute("ratings", BookControllerHtmx.getRatings(""));
        model.addAttribute("authors", getAuthors());
        model.addAttribute(GENRES, getGenres());
        model.addAttribute("reviewers", getReviewers(principal));
        addUserToModel(principal, model);
        model.addAttribute("paginationLink", "find?reviewer=" + reviewer);

        if (hxRequest) {
            response.addHeader("HX-Trigger-After-Swap", "clearSelectReviewer");
            return "find-reviews :: cloudy-find-by-results";
        } else {
            return "find-reviews";
        }
    }

    @GetMapping(value = {"/useradmin"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String userAdmin(Model model, Principal principal) {

        model.addAttribute( USERS, userRepository.findAll());
        addUserToModel(principal, model);

        return "user-admin";
    }

    @DeleteMapping(value = "/deleteuser/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String deleteUserById(@PathVariable String id, Principal principal, Model model,
                                 HttpServletResponse response) {

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {
            if (user.get().getId().equals(id)) {
                LOGGER.info("User {} on {} attempted to delete themselves. This isn't allowed",
                        user.get().getFullName(), user.get().getAuthProvider());

                model.addAttribute(USERS, userRepository.findAll());
                addUserToModel(principal, model);
                response.addHeader(HX_TRIGGER_AFTER_SWAP, "{ \"showFlashMessage\": {\"level\": \"warn\", \"message\": \"You cannot delete your own logged on user\"}}");
                response.addHeader(HX_RETARGET, USERS_TABLE);
                return USER_ADMIN_CLOUDY_USER_ADMIN_TABLE;
            }

            userRepository.deleteById(id);

            model.addAttribute(USERS, userRepository.findAll());
            addUserToModel(principal, model);
            response.addHeader(HX_TRIGGER_AFTER_SWAP, "{ \"showFlashMessage\": {\"level\": \"info\", \"message\": \"Selected user successfully deleted\"}}");
            response.addHeader("HX-Trigger-After-Settle", "refreshSimpleDataTables");

        } else {
            model.addAttribute(USERS, userRepository.findAll());
            addUserToModel(principal, model);
            response.addHeader(HX_TRIGGER_AFTER_SWAP, "{ \"showFlashMessage\": {\"level\": \"warn\", \"message\": \"Your userid no longer found in the data store!\"}}");
            response.addHeader(HX_RETARGET, USERS_TABLE);
        }
        return  USER_ADMIN_CLOUDY_USER_ADMIN_TABLE;
    }

    @PutMapping(value = "/updateuserrole/{id}", params = {"role"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String updateUserRoles(@PathVariable String id, @RequestParam String role,
                                  Principal principal, Model model,
                                  HttpServletResponse response) {

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);
        if (user.isPresent()) {
            if (user.get().getId().equals(id)) {
                LOGGER.info("User {} on {} attempted to change their own roles. This isn't allowed",
                        user.get().getFullName(), user.get().getAuthProvider());
                model.addAttribute(USERS, userRepository.findAll());
                addUserToModel(principal, model);
                response.addHeader(HX_TRIGGER_AFTER_SWAP, "{ \"showFlashMessage\": {\"level\": \"warn\", \"message\": \"Your cannot change the role for you own logged on user!\"}}");
                response.addHeader(HX_RETARGET, USERS_TABLE);
                return USER_ADMIN_CLOUDY_USER_ADMIN_TABLE;
            }

            ClientRoles roles = new ClientRoles();
            roles.setId(id);
            if (role.equalsIgnoreCase("ROLE_ADMIN")) {
                roles.setAdmin(true);
                roles.setEditor(true);
            } else if (role.equalsIgnoreCase("ROLE_EDITOR")) {
                roles.setAdmin(false);
                roles.setEditor(true);
            } else {
                roles.setAdmin(false);
                roles.setEditor(false);
            }
            userRepository.updateUserRoles(roles);
            User updatedUser = userRepository.findById(id).orElseThrow(() ->
                    new IllegalArgumentException("Didn't find the user being updated"));

            List<User> singleUser = new ArrayList<>();
            singleUser.add(updatedUser);
            model.addAttribute(USERS, singleUser);
            addUserToModel(principal, model);
            response.addHeader(HX_TRIGGER_AFTER_SWAP, "{ \"showFlashMessage\": {\"level\": \"info\", \"message\": \"" + updatedUser.getFullName() + "'s role updated OK\"}}");
            return "user-admin :: cloudy-user-admin-row";

        } else {
            model.addAttribute(USERS, userRepository.findAll());
            addUserToModel(principal, model);
            response.addHeader(HX_TRIGGER_AFTER_SWAP, "{ \"showFlashMessage\": {\"level\": \"warn\", \"message\": \"Your userid no longer found in the data store!\"}}");
            response.addHeader(HX_RETARGET, USERS_TABLE);
            return USER_ADMIN_CLOUDY_USER_ADMIN_TABLE;
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
