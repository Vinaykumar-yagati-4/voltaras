package com.voltaras.authservice.util;

import com.voltaras.authservice.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight, in-memory sliding-window rate limiter used to protect
 * the public forgot-password endpoint from repeated abuse.
 *
 * No external dependency is required. Limits are configurable via
 * {@code app.password-reset.rate-limit.*} and can be disabled entirely
 * (tests / low-traffic environments).
 */
@Component
public class PasswordResetRateLimiter {

    private final Map<String, Deque<Long>> attemptsByKey =
            new ConcurrentHashMap<>();

    @Value("${app.password-reset.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.password-reset.rate-limit.max-per-window:3}")
    private int maxPerWindow;

    @Value("${app.password-reset.rate-limit.window-seconds:900}")
    private long windowSeconds;

    /**
     * Registers an attempt for the given key (normalized email) and
     * throws {@link RateLimitExceededException} when the configured
     * maximum number of attempts within the window is exceeded.
     */
    public void check(String key) {

        if (!enabled) {
            return;
        }

        long now = System.currentTimeMillis();
        long cutoff = now - windowSeconds * 1000L;

        Deque<Long> timestamps =
                attemptsByKey.computeIfAbsent(
                        key,
                        k -> new ArrayDeque<>()
                );

        synchronized (timestamps) {

            while (!timestamps.isEmpty()
                    && timestamps.peekFirst() < cutoff) {

                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxPerWindow) {

                throw new RateLimitExceededException(
                        "Too many password reset requests. Please try again later."
                );
            }

            timestamps.addLast(now);
        }

        pruneIfNeeded();
    }

    /**
     * Prevents unbounded growth of the map when keys stop being used.
     */
    private void pruneIfNeeded() {

        if (attemptsByKey.size() > 10_000) {

            attemptsByKey.entrySet()
                    .removeIf(entry -> entry.getValue().isEmpty());
        }
    }
}
