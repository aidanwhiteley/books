package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.dtos.LayoutAnalyticsConfig;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import static org.springframework.util.StringUtils.hasText;

@ControllerAdvice(assignableTypes = {BookControllerHtmx.class, BookSecureControllerHtmx.class})
public class LayoutTemplateControllerAdvice {

    private final Environment environment;

    public LayoutTemplateControllerAdvice(Environment environment) {
        this.environment = environment;
    }

    @ModelAttribute("layoutAnalyticsConfig")
    public LayoutAnalyticsConfig addLayoutAnalyticsConfig() {
        String umamiWebsiteId = environment.getProperty("books.analytics.umami.websiteId");
        return new LayoutAnalyticsConfig(
                hasText(umamiWebsiteId),
                umamiWebsiteId,
                environment.getProperty("books.analytics.umami.doNotTrack"),
                environment.getProperty("books.analytics.umami.domains"),
                environment.getProperty("books.analytics.umami.host-url"));
    }
}
