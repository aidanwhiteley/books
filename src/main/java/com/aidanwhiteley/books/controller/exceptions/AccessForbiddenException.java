package com.aidanwhiteley.books.controller.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@SuppressWarnings("serial")
@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class AccessForbiddenException  extends RuntimeException {
    public AccessForbiddenException(String msg) {
        super(msg);
    }
}
