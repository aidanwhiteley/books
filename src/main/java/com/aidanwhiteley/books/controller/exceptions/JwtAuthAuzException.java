package com.aidanwhiteley.books.controller.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class JwtAuthAuzException extends RuntimeException {
    public JwtAuthAuzException(String msg) {
        super(msg);
    }

}