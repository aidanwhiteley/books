package com.aidanwhiteley.books.controller.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class NotAuthorisedException extends RuntimeException {
    public NotAuthorisedException(String msg) {
        super(msg);
    }
}
