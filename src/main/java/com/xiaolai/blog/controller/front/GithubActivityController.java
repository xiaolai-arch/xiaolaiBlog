package com.xiaolai.blog.controller.front;

import com.xiaolai.blog.common.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub 年度贡献热力图数据源。
 * 服务器侧抓取 github.com/users/{user}/contributions 页面并解析每日贡献次数，
 * 内存缓存 3 小时（失败短缓存 10 分钟），前端不直连 GitHub。
 */
@RestController("frontGithubActivityController")
@RequestMapping("/api/v1/github")
public class GithubActivityController {

    private static final Pattern DAY_RE = Pattern.compile(
            "<td[^>]*?\\bdata-date=\"(\\d{4}-\\d{2}-\\d{2})\"[^>]*?>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LEVEL_RE = Pattern.compile("\\bdata-level=\"(\\d+)\"");
    private static final Pattern ID_RE = Pattern.compile("\\bid=\"([^\"]+)\"");
    private static final Pattern TIP_RE = Pattern.compile(
            "<tool-tip[^>]*\\bfor=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</tool-tip>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TIP_COUNT_RE = Pattern.compile("([\\d,]+)\\s+contributions?",
            Pattern.CASE_INSENSITIVE);
    private static final String UA =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Safari/537.36";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** 简易内存缓存：user -> (到期时间, 数据) */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(long expireAt, List<DayData> data) {}
    public record DayData(String date, int count, int level) {}

    @GetMapping("/contributions")
    public Result<List<DayData>> contributions(
            @RequestParam(value = "user", defaultValue = "xiaolai-arch") String user) {
        String key = user.replaceAll("[^A-Za-z0-9-]", "");
        if (key.isBlank()) return Result.badRequest("Invalid user");

        long now = System.currentTimeMillis();
        CacheEntry hit = cache.get(key);
        if (hit != null && now < hit.expireAt()) {
            return Result.success(hit.data());
        }

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://github.com/users/" + key + "/contributions"))
                    .header("User-Agent", UA)
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) throw new IllegalStateException("GitHub HTTP " + resp.statusCode());

            List<DayData> days = parse(resp.body());
            if (days.isEmpty()) throw new IllegalStateException("No contribution cells parsed");

            cache.put(key, new CacheEntry(now + Duration.ofHours(3).toMillis(), days));
            return Result.success(days);
        } catch (Exception e) {
            // 失败短缓存，避免频繁打 GitHub
            CacheEntry last = hit; // 过期旧数据也聊胜于无
            if (last != null && last.data() != null && !last.data().isEmpty()) {
                cache.put(key, new CacheEntry(now + Duration.ofMinutes(10).toMillis(), last.data()));
                return Result.success(last.data());
            }
            cache.put(key, new CacheEntry(now + Duration.ofMinutes(10).toMillis(), List.of()));
            return Result.error(502, "Failed to load GitHub contributions");
        }
    }

    /**
     * 解析贡献页：日历格子是 <td data-date data-level id="contribution-day-component-..">，
     * 贡献次数在同 id 的 <tool-tip> 文案里（"N contributions on ..." / "No contributions on ..."）。
     * level 直接用 GitHub 自己算好的 0-4 档。
     */
    static List<DayData> parse(String html) {
        Map<String, Integer> tipCounts = new java.util.HashMap<>();
        Matcher tips = TIP_RE.matcher(html);
        while (tips.find()) {
            Matcher c = TIP_COUNT_RE.matcher(tips.group(2));
            tipCounts.put(tips.group(1), c.find()
                    ? Integer.parseInt(c.group(1).replace(",", ""))
                    : 0);
        }

        // 主日历格子带 aria-describedby；页面内嵌的多年份迷你月历是模板格子。
        // 优先采信主日历数据，最终按日期排序输出。
        java.util.TreeMap<String, DayData> byDate = new java.util.TreeMap<>();
        Matcher m = DAY_RE.matcher(html);
        while (m.find()) {
            String tag = m.group();
            int level = 0;
            Matcher lv = LEVEL_RE.matcher(tag);
            if (lv.find()) level = Math.min(4, Math.max(0, Integer.parseInt(lv.group(1))));
            int count = 0;
            Matcher id = ID_RE.matcher(tag);
            if (id.find()) count = tipCounts.getOrDefault(id.group(1), 0);
            DayData nd = new DayData(m.group(1), count, level);
            DayData prev = byDate.get(m.group(1));
            if (prev == null || tag.contains("aria-describedby")) {
                byDate.put(m.group(1), nd);
            }
        }
        return new ArrayList<>(byDate.values());
    }

    static int levelOf(int count) {
        if (count <= 0) return 0;
        if (count <= 2) return 1;
        if (count <= 4) return 2;
        if (count <= 7) return 3;
        return 4;
    }
}
