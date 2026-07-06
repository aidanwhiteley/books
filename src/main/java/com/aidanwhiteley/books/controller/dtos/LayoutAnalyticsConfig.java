package com.aidanwhiteley.books.controller.dtos;

public record LayoutAnalyticsConfig(boolean umamiEnabled, String umamiWebsiteId, String umamiDoNotTrack,
                                    String umamiDomains, String umamiHostUrl) {
}
