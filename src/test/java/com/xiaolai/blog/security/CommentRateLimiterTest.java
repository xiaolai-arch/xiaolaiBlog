package com.xiaolai.blog.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class CommentRateLimiterTest {

    private final AtomicLong now = new AtomicLong(1_000_000L);
    private final CommentRateLimiter limiter = new CommentRateLimiter(now::get);

    @Test
    void blocksAfterLimitWithinWindow() {
        for (int i = 0; i < CommentRateLimiter.MAX_REQUESTS; i++) {
            assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();
        }
        assertThat(limiter.tryAcquire("1.2.3.4")).isFalse();
    }

    @Test
    void allowsAgainOnceWindowPasses() {
        for (int i = 0; i < CommentRateLimiter.MAX_REQUESTS; i++) {
            limiter.tryAcquire("1.2.3.4");
        }
        now.addAndGet(CommentRateLimiter.WINDOW_MILLIS);

        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();
    }

    @Test
    void keysAreIndependent() {
        for (int i = 0; i < CommentRateLimiter.MAX_REQUESTS; i++) {
            limiter.tryAcquire("1.2.3.4");
        }
        assertThat(limiter.tryAcquire("5.6.7.8")).isTrue();
    }

    @Test
    void clientIpPrefersNginxHeader() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("127.0.0.1");
        assertThat(CommentRateLimiter.clientIp(req)).isEqualTo("127.0.0.1");

        req.addHeader("X-Real-IP", "203.0.113.9");
        assertThat(CommentRateLimiter.clientIp(req)).isEqualTo("203.0.113.9");
    }
}
