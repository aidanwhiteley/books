package com.aidanwhiteley.books.controller.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ApiExceptionData {

	private final int code;
	private final String error;
	private final String message;
	private final String uri;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
	private LocalDateTime dateStamp = LocalDateTime.now();
}
