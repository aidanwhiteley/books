package com.aidanwhiteley.books.controller.dtos;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ApiExceptionData {

    final private int code;
    final private String error;
    final private String message;
    final private String uri;
    private LocalDateTime dateStamp = LocalDateTime.now();
}
