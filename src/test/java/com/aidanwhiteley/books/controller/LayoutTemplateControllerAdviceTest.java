package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.controller.dtos.LayoutAnalyticsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class LayoutTemplateControllerAdviceTest {

    @Test
    void testAddLayoutAnalyticsConfig() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("books.analytics.umami.websiteId", "site-123")
                .withProperty("books.analytics.umami.doNotTrack", "true")
                .withProperty("books.analytics.umami.domains", "example.com")
                .withProperty("books.analytics.umami.host-url", "https://stats.example.com");

        LayoutTemplateControllerAdvice advice = new LayoutTemplateControllerAdvice(environment);
        LayoutAnalyticsConfig config = advice.addLayoutAnalyticsConfig();

        assertTrue(config.umamiEnabled());
        assertEquals("site-123", config.umamiWebsiteId());
        assertEquals("true", config.umamiDoNotTrack());
        assertEquals("example.com", config.umamiDomains());
        assertEquals("https://stats.example.com", config.umamiHostUrl());
    }

    @Test
    void testAddLayoutAnalyticsConfigWithoutWebsiteId() {
        MockEnvironment environment = new MockEnvironment();

        LayoutTemplateControllerAdvice advice = new LayoutTemplateControllerAdvice(environment);
        LayoutAnalyticsConfig config = advice.addLayoutAnalyticsConfig();

        assertFalse(config.umamiEnabled());
        assertNull(config.umamiWebsiteId());
    }
}
