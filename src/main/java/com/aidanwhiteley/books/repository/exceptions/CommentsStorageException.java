package com.aidanwhiteley.books.repository.exceptions;

import java.io.Serial;

public class CommentsStorageException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CommentsStorageException(String msg) {
        super(msg);
    }
}
