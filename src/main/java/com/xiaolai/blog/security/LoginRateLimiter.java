package com.xiaolai.blog.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 登录接口按 IP 的滑动窗口限频：账号锁定是按账号的，攻击者可以换着用户名
 * 喷洒；此限频器按来源 IP 兜底。单实例内存实现，重启即清零。
 */
@Component
public class LoginRateLimiter {

    /** 10 分钟窗口内最多 20 次尝试（正常人多账号/手滑也够用）。 */
    static final int MAX_ATTEMPTS = 20;
    static final long WINDOW_MILLIS = 10 * 60 * 1000L;
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public LoginRateLimiter() {
        this(System::currentTimeMillis);
    }

    LoginRateLimiter(LongSupplier clock) {
        this.clock = clock;
    }

    /** 记录一次登录尝试；返回 false 表示该 IP 已超限，应拒绝。 */
    public boolean tryAcquire(String ip) {
        long now = clock.getAsLong();
        if (hits.size() > MAX_TRACKED_KEYS) {
            evictExpired(now);
        }
        Deque<Long> window = hits.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() >= WINDOW_MILLIS) {
                window.pollFirst();
            }
            if (window.size() >= MAX_ATTEMPTS) {
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

    public static String clientIp(HttpServletRequest request) {
        return CommentRateLimiter.clientIp(request);
    }
}
