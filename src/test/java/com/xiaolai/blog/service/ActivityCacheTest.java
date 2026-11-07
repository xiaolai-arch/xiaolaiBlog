package com.xiaolai.blog.service;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class ActivityCacheTest {
    @Test
    void negativeCachePreventsRepeatedExternalCallsUntilExpiry() {
        AtomicLong clock = new AtomicLong(0);
        AtomicInteger attempts = new AtomicInteger();
        ActivityCache cache = new ActivityCache(clock::get, Executors.newSingleThreadExecutor(), 10);
        try {
            Callable<List<String>> load = () -> { attempts.incrementAndGet(); throw new IllegalStateException("offline"); };
            assertEquals(502, cache.get("a", List::of, load).getCode());
            assertEquals(502, cache.get("a", List::of, load).getCode());
            assertEquals(1, attempts.get());
            clock.set(600001);
            cache.get("a", List::of, load);
            assertEquals(2, attempts.get());
        } finally { cache.close(); }
    }

    @Test
    void concurrentColdRequestsShareOneFetch() throws Exception {
        ActivityCache cache = new ActivityCache(System::currentTimeMillis, Executors.newFixedThreadPool(2), 10);
        ExecutorService callers = Executors.newFixedThreadPool(2);
        CountDownLatch started = new CountDownLatch(1), finish = new CountDownLatch(1);
        AtomicInteger attempts = new AtomicInteger();
        Callable<List<String>> load = () -> {
            attempts.incrementAndGet(); started.countDown();
            assertTrue(finish.await(3, TimeUnit.SECONDS));
            return List.of("public data");
        };
        try {
            var first = callers.submit(() -> cache.get("a", List::of, load));
            assertTrue(started.await(3, TimeUnit.SECONDS));
            var second = callers.submit(() -> cache.get("a", List::of, load));
            finish.countDown();
            assertEquals(List.of("public data"), first.get(3, TimeUnit.SECONDS).getData());
            assertEquals(List.of("public data"), second.get(3, TimeUnit.SECONDS).getData());
            assertEquals(1, attempts.get());
        } finally { callers.shutdownNow(); cache.close(); }
    }
}
