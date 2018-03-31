package com.aidanwhiteley.books.controller.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@SuppressWarnings("serial")
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class IllegalArgumentException extends RuntimeException {
	@SuppressWarnings("unused")
	public IllegalArgumentException(String msg) {
		super(msg);
	}

}
