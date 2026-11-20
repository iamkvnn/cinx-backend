package com.cinx.reporting.analytics.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * At most {@link #PER_HOUR} events per user and clock hour, per reporting instance. The gateway has no rate limiter, so
 * this is what keeps one client from flooding the table.
 */
@Component
@RequiredArgsConstructor
public class EventRateLimiter {
    public static final int PER_HOUR = 600;
    private static final int MAX_TRACKED_USERS = 50_000;

    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /** Takes {@code events} from the user's hour, or nothing when they do not all fit. */
    public boolean tryUse(String userId, int events) {
        long hour = clock.millis() / 3_600_000L;
        if (windows.size() > MAX_TRACKED_USERS) {
            windows.values().removeIf(window -> window.hour() != hour);
        }
        boolean[] allowed = {false};
        windows.compute(userId, (id, window) -> {
            Window current = window == null || window.hour() != hour ? new Window(hour, 0) : window;
            if (current.used() + events > PER_HOUR) return current;
            allowed[0] = true;
            return new Window(hour, current.used() + events);
        });
        return allowed[0];
    }

    private record Window(long hour, int used) {
    }
}
