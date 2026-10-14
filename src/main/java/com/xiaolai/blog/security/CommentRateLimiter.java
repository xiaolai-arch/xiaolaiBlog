package com.xiaolai.blog.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * In-memory sliding-window limiter for anonymous comment submission.
 * Single-instance only: counters are lost on restart and not shared across nodes.
 */
@Component
public class CommentRateLimiter {

    static final int MAX_REQUESTS = 5;
    static final long WINDOW_MILLIS = 10 * 60 * 1000L;
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public CommentRateLimiter() {
        this(System::currentTimeMillis);
    }

    CommentRateLimiter(LongSupplier clock) {
        this.clock = clock;
    }

    /** Records an attempt for {@code key}; returns false if the key is over the limit. */
    public boolean tryAcquire(String key) {
        long now = clock.getAsLong();
        if (hits.size() > MAX_TRACKED_KEYS) {
            evictExpired(now);
        }
        Deque<Long> window = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() >= WINDOW_MILLIS) {
                window.pollFirst();
            }
            if (window.size() >= MAX_REQUESTS) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }

    private void evictExpired(long now) {
        hits.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                Long last = e.getValue().peekLast();
                return last == null || now - last >= WINDOW_MILLIS;
            }
        });
    }

    /**
     * Nginx overwrites X-Real-IP with $remote_addr, so it is trustworthy only when port 8080
     * is reachable from localhost alone.
     */
    public static String clientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
