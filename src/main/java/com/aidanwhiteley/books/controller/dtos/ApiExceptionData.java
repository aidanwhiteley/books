package com.aidanwhiteley.books.controller.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ApiExceptionData {

    final private int code;
    final private String error;
    final private String message;
    final private String uri;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
    private LocalDateTime dateStamp = LocalDateTime.now();
}
