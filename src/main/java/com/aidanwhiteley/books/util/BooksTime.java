package com.aidanwhiteley.books.util;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

public final class BooksTime {

    private static volatile Clock CLOCK = Clock.systemDefaultZone();

    private BooksTime() {
    }

    static void setClock(Clock clock) {
        CLOCK = Objects.requireNonNull(clock);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(clock());
    }

    public static Clock clock() {
        return CLOCK;
    }

    public static ZoneId zoneId() {
        return clock().getZone();
    }
}
