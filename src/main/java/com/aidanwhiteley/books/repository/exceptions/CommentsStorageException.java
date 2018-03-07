package com.aidanwhiteley.books.repository.exceptions;

public class CommentsStorageException  extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CommentsStorageException(String msg) {
        super(msg);
    }
}
