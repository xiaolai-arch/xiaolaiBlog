package com.xiaolai.blog.service;

import com.xiaolai.blog.common.response.Result;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Bounded single-flight cache. Failed refreshes retain stale public data. */
@Component
public class ActivityCache {
    private record Entry(long expires, List<?> data) {}
    private final Map<String, Entry> entries = new HashMap<>();
    private final Map<String, CompletableFuture<List<?>>> pending = new HashMap<>();
    private final LongSupplier clock;
    private final ExecutorService workers;
    private final int capacity;

    public ActivityCache() {
        this(System::currentTimeMillis, new ThreadPoolExecutor(2, 4, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(32), runnable -> {
                    Thread thread = new Thread(runnable, "activity-refresh");
                    thread.setDaemon(true);
                    return thread;
                }), 100);
    }

    public ActivityCache(LongSupplier clock, ExecutorService workers, int capacity) {
        this.clock = clock;
        this.workers = workers;
        this.capacity = capacity;
    }

    @SuppressWarnings("unchecked")
    public <T> Result<List<T>> get(String key, Supplier<List<T>> disk, Callable<List<T>> load) {
        CompletableFuture<List<?>> future;
        List<T> stale;
        synchronized (this) {
            long now = clock.getAsLong();
            Entry hit = entries.get(key);
            if (hit != null && now < hit.expires()) {
                return hit.data().isEmpty() ? Result.error(502, "外部服务暂时不可用，请稍后重试")
                        : Result.success((List<T>) hit.data());
            }
            stale = hit == null ? List.of() : (List<T>) hit.data();
            if (stale.isEmpty()) stale = disk.get();
            future = pending.get(key);
            if (future == null) {
                entries.entrySet().removeIf(entry -> !entry.getKey().equals(key)
                        && entry.getValue().expires() <= now && !pending.containsKey(entry.getKey()));
                if (!entries.containsKey(key) && entries.size() + pending.size() >= capacity) {
                    return Result.error(429, "活跃度查询繁忙，请稍后重试");
                }
                final List<T> fallback = List.copyOf(stale);
                future = new CompletableFuture<>();
                final CompletableFuture<List<?>> task = future;
                pending.put(key, task);
                try {
                    workers.execute(() -> {
                        List<T> data;
                        long ttl;
                        try {
                            data = List.copyOf(load.call());
                            ttl = Duration.ofHours(3).toMillis();
                        } catch (Exception e) {
                            data = fallback;
                            ttl = Duration.ofMinutes(10).toMillis();
                        }
                        synchronized (ActivityCache.this) {
                            entries.put(key, new Entry(clock.getAsLong() + ttl, data));
                            pending.remove(key);
                        }
                        task.complete(data);
                    });
                } catch (RejectedExecutionException e) {
                    pending.remove(key);
                    entries.put(key, new Entry(now + Duration.ofMinutes(1).toMillis(), fallback));
                    task.complete(fallback);
                }
            }
        }
        if (!stale.isEmpty()) return Result.success(stale);
        try {
            List<T> data = (List<T>) future.get(20, TimeUnit.SECONDS);
            return data.isEmpty() ? Result.error(502, "外部服务暂时不可用，请稍后重试") : Result.success(data);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error(503, "查询已中断");
        } catch (ExecutionException | TimeoutException e) {
            return Result.error(503, "活跃度正在更新，请稍后重试");
        }
    }

    @PreDestroy
    public void close() { workers.shutdownNow(); }
}
