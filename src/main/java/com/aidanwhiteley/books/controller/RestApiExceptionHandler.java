package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.dtos.ApiExceptionData;
import com.aidanwhiteley.books.controller.exceptions.AccessForbiddenException;
import com.aidanwhiteley.books.controller.exceptions.NotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestApiExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String MESSAGE_NO_ACCESS = "Sorry - you do not have access to the URL you requested";
    public static final String MESSAGE_NOT_FOUND = "Sorry - the resource your tried to access cannot be found";
    public static final String MESSAGE_ILLEGAL_ARGUMENT = "Sorry - you supplied an unrecorgnised input parameter value. Please check and try again";
    public static final String MESSAGE_ROUTE_NOT_FOUND = "Sorry - the URL you asked for is not known to the application";

    @ExceptionHandler({ AccessDeniedException.class, AccessForbiddenException.class })
    public ResponseEntity<Object> handleAccessDeniedForbiddenException(Exception ex, WebRequest request) {

        return new ResponseEntity<Object>(
                new ApiExceptionData(HttpStatus.FORBIDDEN.value(),
                        HttpStatus.FORBIDDEN.getReasonPhrase(),
                        MESSAGE_NO_ACCESS + " : " + ex.getLocalizedMessage(),
                        request.getDescription(false)),
                new HttpHeaders(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({ NotFoundException.class })
    public ResponseEntity<Object> handleNotFoundException(Exception ex, WebRequest request) {

        return new ResponseEntity<Object>(
                new ApiExceptionData(HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        MESSAGE_NOT_FOUND + " : " + ex.getLocalizedMessage(),
                        request.getDescription(false)),
                new HttpHeaders(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({ IllegalArgumentException.class, NumberFormatException.class })
    public ResponseEntity<Object> handleIllegalArgumentException(Exception ex, WebRequest request) {

        return new ResponseEntity<Object>(
                new ApiExceptionData(HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        MESSAGE_ILLEGAL_ARGUMENT + " : " + ex.getLocalizedMessage(),
                        request.getDescription(false)),
                new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

}
