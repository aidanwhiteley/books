package com.aidanwhiteley.books.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.ZoneId;

@Component
public class BooksTimeConfig {

    public BooksTimeConfig(@Value("${books.time.zone-id}") String zoneId) {
        BooksTime.setClock(Clock.system(ZoneId.of(zoneId)));
    }
}
