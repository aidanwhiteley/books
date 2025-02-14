package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.exceptions.JwtAuthAuzException;
import com.aidanwhiteley.books.controller.exceptions.NotAuthorisedException;
import com.aidanwhiteley.books.controller.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public interface BookControllerHtmxExceptionHandling {

    Logger LOGGER = LoggerFactory.getLogger(BookControllerHtmxExceptionHandling.class);

    // The REST API part of this application registers a global @RestControllerAdvice to centrally handle exceptions
    // and they generally return JSON to the client.
    // As we want to leave the REST API in place, we handle exceptions locally in this HTMX based controller
    // so that we can return HTML views for any errors from this controller.

    @ExceptionHandler(UncategorizedMongoDbException.class)
    default String handleInMemoryMongoFullTextSearchException(UncategorizedMongoDbException ex, Model model,
                                                              Principal principal, WebRequest request) {
        LOGGER.error("An UncategorizedMongoDbException occurred. This is normally expected when running in " +
                "development mode when trying to use the Search as full text indexes aren't supported by " +
                "the in memory fake Mongo. However, as it is just possible that it could happen for other " +
                "reasons, the full stack trace is logged", ex);
        String description = "Search doesn't work when running against the in-memory Mongo " +
                "used in development because full text indexes are not supported in that implementation.";
        return addAttributesToErrorPage(description, "e-mongo-uncategorized", model, principal, request);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({IllegalArgumentException.class, NumberFormatException.class, MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class})
    default String handleIllegalArgumentException(Exception ex, Model model, Principal principal, WebRequest request) {
        LOGGER.error("An unacceptable input was received. Either this is an application error or someone manually sending incorrect parameters", ex);
        String description = "Sorry - the values sent to the application are not acceptable.";
        return addAttributesToErrorPage(description, "e-400", model, principal, request);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    default String handleNotFoundException(NotFoundException ex, Model model, Principal principal, WebRequest request) {
        LOGGER.error("The application couldn't find the resource requested - {}", ex.getMessage(), ex);
        String description = "Sorry - the application could not find what you wanted";
        return addAttributesToErrorPage(description, "e-404", model, principal, request);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(NotAuthorisedException.class)
    default String handleNotAuthorisedException(NotAuthorisedException ex, Model model, Principal principal, WebRequest request) {
        LOGGER.error("An attempt was made to access a protected resource without the required authorisation - {}", ex.getMessage(), ex);
        String description = "Sorry - you are not authorised to access this functionality";
        return addAttributesToErrorPage(description, "e-401", model, principal, request);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(JwtAuthAuzException.class)
    default String handleJwtAuthAuzException(JwtAuthAuzException ex, Model model, Principal principal, WebRequest request) {
        LOGGER.error("There was a problem with the JWT token process - {}", ex.getMessage(), ex);
        String description = "Sorry - there was problem with processing your logon token";
        return addAttributesToErrorPage(description, "e-401", model, principal, request);
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    default String handleAccessDeniedException(AccessDeniedException ex, Model model, Principal principal, WebRequest request) {
        LOGGER.error("An attempt was made to access a protected resource without the required permission - {}", ex.getMessage(), ex);
        String description = "Sorry - you are not permitted to access this functionality";
        return addAttributesToErrorPage(description, "e-403", model, principal, request);
    }

    // Exception handler of last resort!
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    default String handleException(Exception ex, Model model, Principal principal, WebRequest request) {
        LOGGER.error("An unhandled exception was caught by the exception handler of last resort - {}", ex.getMessage(), ex);
        String description = "Sorry - an unexpected problem occurred in the application.";
        return addAttributesToErrorPage(description, "e-500", model, principal, request);
    }

    private String addAttributesToErrorPage(String description, String code, Model model, Principal principal, WebRequest request) {
        model.addAttribute("description", description);
        model.addAttribute("code", code);
        model.addAttribute("dateTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        addUserToModel(principal, model);

        if (request.getHeader("hx-request") != null && request.getHeader("Hx-request").equals("true")) {
            return "error :: cloudy-error-detail";
        } else {
            return "error";
        }
    }

    void addUserToModel(Principal principal, Model model);

}
