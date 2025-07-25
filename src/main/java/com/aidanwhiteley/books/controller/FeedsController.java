package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.exceptions.NotFoundException;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.service.GoodReadsExportService;
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;
import com.aidanwhiteley.books.util.SiteRssFeed;
import com.rometools.rome.feed.rss.Channel;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.Principal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("/feeds")
public class FeedsController {

    private final SiteRssFeed siteRssFeed;
    private final GoodReadsExportService goodReadsExportService;
    private final JwtAuthenticationUtils authUtils;

    private static final Logger LOGGER = LoggerFactory.getLogger(FeedsController.class);

    public FeedsController(SiteRssFeed siteRssFeed, GoodReadsExportService goodReadsExportService,
                           JwtAuthenticationUtils authUtils) {
        this.siteRssFeed = siteRssFeed;
        this.goodReadsExportService = goodReadsExportService;
        this.authUtils = authUtils;
    }

    @GetMapping(value = "/rss")
    public Channel findRecentActivity() {
        return siteRssFeed.createSiteRssFeed();
    }

    @GetMapping("/exportbooks")
    public void exportToCSV(HttpServletResponse response, Principal principal) {

        Optional<User> user = authUtils.extractUserFromPrincipal(principal, false);

        if (user.isPresent()) {
                try {
                    response.setContentType("text/csv");
                    DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                    String currentDateTime = dateFormatter.format(new Date());

                    String headerKey = "Content-Disposition";
                    String headerValue = "attachment; filename=cloudy_book_club_export_" + currentDateTime + ".csv";
                    response.setHeader(headerKey, headerValue);

                    var responseStream = response.getOutputStream();
                    // The logged on user can only export their own books reviews
                    var csvRows = goodReadsExportService.getExportInGoodReadsFormat(user.get());
                    for (int i = 0; i < csvRows.size(); i++) {
                        if (i < csvRows.size() - 1) {
                            responseStream.println(csvRows.get(i));
                        } else {
                            responseStream.print(csvRows.get(i));
                        }
                    }
                } catch (IOException ioe) {
                    LOGGER.error("There was an unexpected error creating a Goodreads format export", ioe);
                    try {
                        response.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    } catch (IOException ioe2) {
                        // Give up trying to tell the user about it!
                        LOGGER.error("Cannot send an error status code while creating a Goodreads format export", ioe2);
                    }
                }

        } else {
            LOGGER.error("A user that doesn't exist in the database was trying to export book reviews - {}", user);
            throw new NotFoundException("User not found when trying export books");
        }


    }
}
