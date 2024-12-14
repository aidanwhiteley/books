package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.service.StatsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BookControllerHtmx {

    private final BookRepository bookRepository;

    private final StatsService statsService;

    public BookControllerHtmx(BookRepository bookRepository, StatsService statsService) {
        this.bookRepository = bookRepository;
        this.statsService = statsService;
    }

    @GetMapping(value = "/index")
    public String index() {
        return "index";
    }
}
