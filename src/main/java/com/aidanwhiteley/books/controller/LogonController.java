package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
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

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class LogonController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogonController.class);

    @Autowired
    private UserRepository userRepository;

    @Value("${books.client.postLogonUrl}")
    private String postLogonUrl;

    @RequestMapping(value = "/logonWithGoogle", method = GET)
    public ResponseEntity logonWithGoogle(Principal principal) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(postLogonUrl));

        createOrUpdateUserFromGoogleAuth(principal);

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    private void createOrUpdateUserFromGoogleAuth(Principal principal) {

        OAuth2Authentication auth = (OAuth2Authentication) principal;

        @SuppressWarnings("unchecked")
        Map<String, String> userDetails = (LinkedHashMap) auth.getDetails();

        String id = userDetails.get("id");

        List<User> googleUsers = userRepository.findAllByAuthenticationServiceId(id);

        User googleUser;

        if (googleUsers.size() == 0) {

            googleUser = User.builder().authenticationServiceId(userDetails.get("id")).
                    firstName(userDetails.get("given_name")).
                    lastName(userDetails.get("family_name")).
                    fullName(userDetails.get("name")).
                    link(userDetails.get("link")).
                    picture(userDetails.get("picture")).
                    email(userDetails.get("email")).
                    lastLogon(LocalDateTime.now()).
                    firstLogon(LocalDateTime.now()).
                    authProvider(User.AuthenticationProvider.GOOGLE).
                    role(User.Role.ROLE_USER).
                    build();

            userRepository.insert(googleUser);
            LOGGER.info("User saved to repository: " + googleUser);

        } else {
            googleUser = googleUsers.get(0);
            // In case user has made changes on Google e.g. new picture
            googleUser.setFirstName(userDetails.get("given_name"));
            googleUser.setLastName(userDetails.get("family_name"));
            googleUser.setFullName(userDetails.get("name"));
            googleUser.setLink(userDetails.get("link"));
            googleUser.setPicture(userDetails.get("picture"));
            googleUser.setEmail(userDetails.get("email"));
            googleUser.setLastLogon(LocalDateTime.now());
            userRepository.save(googleUser);
            LOGGER.info("User updated in repository: " + googleUser);
        }

    }

}
