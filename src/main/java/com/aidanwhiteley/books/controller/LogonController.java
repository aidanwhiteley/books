package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.exceptions.AccessForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LogonController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogonController.class);

    @RequestMapping(value = "/login")
    public void login() {
        LOGGER.debug("/login called - should have 403d");
        throw new AccessForbiddenException("Must be logged on");
    }
}
