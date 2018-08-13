package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.dtos.ApiExceptionData;
import com.aidanwhiteley.books.controller.exceptions.AccessForbiddenException;
import com.aidanwhiteley.books.controller.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
public class RestApiExceptionHandler extends ResponseEntityExceptionHandler {

	public static final String MESSAGE_NO_ACCESS = "Sorry - you do not have access to the URL you requested";
	public static final String MESSAGE_NOT_FOUND = "Sorry - the resource your tried to access cannot be found";
	public static final String MESSAGE_ILLEGAL_ARGUMENT = "Sorry - you supplied an invalid input parameter value. Please check and try again";
	public static final String MESSAGE_UNEXPECTED_EXCEPTION = "Sorry - an unexpected problem happended - please try later";
	public static final String SAFE_HTML_EXCEPTION_STRING = "SafeHtml";

	private static final Logger API_LOGGER = LoggerFactory.getLogger(RestApiExceptionHandler.class);

	@ExceptionHandler({ AccessDeniedException.class, AccessForbiddenException.class })
	@ResponseStatus(FORBIDDEN)
	public ApiExceptionData handleAccessDeniedForbiddenException(Exception ex, WebRequest request) {

		return new ApiExceptionData(FORBIDDEN.value(), FORBIDDEN.getReasonPhrase(),
				MESSAGE_NO_ACCESS + " : " + ex.getLocalizedMessage(), request.getDescription(false));
	}

	@ExceptionHandler({ NotFoundException.class })
	@ResponseStatus(NOT_FOUND)
	public ApiExceptionData handleNotFoundException(Exception ex, WebRequest request) {

		return new ApiExceptionData(NOT_FOUND.value(), NOT_FOUND.getReasonPhrase(),
				MESSAGE_NOT_FOUND + " : " + ex.getLocalizedMessage(), request.getDescription(false));
	}

	@ExceptionHandler({ IllegalArgumentException.class, NumberFormatException.class,
			MethodArgumentTypeMismatchException.class })
	@ResponseStatus(BAD_REQUEST)
	public ApiExceptionData handleIllegalArgumentException(Exception ex, WebRequest request) {

		return new ApiExceptionData(BAD_REQUEST.value(), BAD_REQUEST.getReasonPhrase(),
				MESSAGE_ILLEGAL_ARGUMENT + " : " + ex.getLocalizedMessage(), request.getDescription(false));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		if (ex.getMessage().contains(SAFE_HTML_EXCEPTION_STRING)) {
			return new ResponseEntity<>(
					new ApiExceptionData(UNSUPPORTED_MEDIA_TYPE.value(), UNSUPPORTED_MEDIA_TYPE.getReasonPhrase(),
							MESSAGE_ILLEGAL_ARGUMENT + " : "
									+ (ex.getBindingResult() != null ? ex.getBindingResult().toString()
											: ex.getMessage()),
							request.getDescription(false)),
					new HttpHeaders(), UNSUPPORTED_MEDIA_TYPE);
		} else {
			return new ResponseEntity<>(
					new ApiExceptionData(BAD_REQUEST.value(), BAD_REQUEST.getReasonPhrase(),
							MESSAGE_ILLEGAL_ARGUMENT + " : "
									+ (ex.getBindingResult() != null ? ex.getBindingResult().toString()
											: ex.getMessage()),
							request.getDescription(false)),
					new HttpHeaders(), BAD_REQUEST);
		}
	}

	/**
	 * Error handler of last resort.
	 * <p>
	 * It also logs the exception. The other exception handlers expect the problem
	 * to have been logged close to where it was initially caught. In this case, it
	 * is likely that the exception may not have already been logged so it is logged
	 * here.
	 */
	@ExceptionHandler({ Exception.class })
	@ResponseStatus(INTERNAL_SERVER_ERROR)
	public ApiExceptionData handleDefaultExceptions(Exception ex, WebRequest request) {

		API_LOGGER.error("An unhandled exception was caught, logged and HTTP status 500 returned to the client.", ex);

		return new ApiExceptionData(INTERNAL_SERVER_ERROR.value(), INTERNAL_SERVER_ERROR.getReasonPhrase(),
				MESSAGE_UNEXPECTED_EXCEPTION + " : " + ex.getLocalizedMessage(), request.getDescription(false));
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatus status, WebRequest request) {

		API_LOGGER.error(
				"The Spring framework rather than the application handled the following exception: " + ex.getMessage(),
				ex);
		return super.handleExceptionInternal(ex, body, headers, status, request);
	}
}
