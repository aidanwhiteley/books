package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.util.OauthAuthenticationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.GOOGLE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class LogonController {

//    private static final Logger LOGGER = LoggerFactory.getLogger(LogonController.class);
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private OauthAuthenticationUtils authUtils;
//
//    @Value("${books.client.postLogonUrl}")
//    private String postLogonUrl;

//    @RequestMapping(value = "/logonWithGoogle", method = GET)
//    public ResponseEntity logonWithGoogle(Principal principal) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setLocation(URI.create(postLogonUrl));
//
//        createOrUpdateUserFromGoogleAuth(principal);
//
//        return new ResponseEntity<>(headers, HttpStatus.FOUND);
//    }



}
