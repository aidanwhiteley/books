package com.aidanwhiteley.books.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class LogonController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogonController.class);

    @Value("${books.client.postLogonUrl}")
    private String postLogonUrl;

    @RequestMapping(value = "/logonToBooks", method = GET)
    public ResponseEntity<?> logon() {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(postLogonUrl));

        LOGGER.info("About to do redirect 1 to {}", postLogonUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }


    @RequestMapping(value = "/postLogin", method = GET, params = { "state", "code" })
    public ResponseEntity<?> postLogin(@RequestParam("state") String state, @RequestParam("code") String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(postLogonUrl));

        LOGGER.info("State was {} and code was {}", state,  code);
        LOGGER.info("About to do redirect 2 to {}", postLogonUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
