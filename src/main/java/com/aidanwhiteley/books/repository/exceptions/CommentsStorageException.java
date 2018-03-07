package com.aidanwhiteley.books.repository.exceptions;

public class CommentsStorageException  extends RuntimeException {
    public CommentsStorageException(String msg) {
        super(msg);
    }
}
