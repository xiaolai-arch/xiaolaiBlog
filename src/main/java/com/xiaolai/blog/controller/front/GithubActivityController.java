package com.xiaolai.blog.controller.front;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.UserProgress;
import com.xiaolai.blog.mapper.UserProgressMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
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

    private final UserProgressMapper progressMapper;

    public GithubActivityController(UserProgressMapper progressMapper) {
        this.progressMapper = progressMapper;
    }

    /** 简易内存缓存：user -> (到期时间, 数据) */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /** 正在后台刷新的 key，防重复触发 */
    private final java.util.Set<String> refreshing = ConcurrentHashMap.newKeySet();

    private record CacheEntry(long expireAt, List<DayData> data) {}
    public record DayData(String date, int count, int level) {}

    @GetMapping("/contributions")
    public Result<List<DayData>> contributions(
            @RequestParam(value = "user", defaultValue = "xiaolai-arch") String user) {
        String key = user.replaceAll("[^A-Za-z0-9-]", "");
        if (key.isBlank()) return Result.badRequest("Invalid user");

        long now = System.currentTimeMillis();
        CacheEntry hit = cache.get(key);
        // 空数据是失败时的负缓存，不拦截——继续走磁盘兜底
        if (hit != null && now < hit.expireAt() && !hit.data().isEmpty()) {
            return Result.success(hit.data());
        }

        // 磁盘上有最后已知数据：立即返回，后台静默刷新（不阻塞请求）
        List<DayData> disk = loadDisk(key);
        if (!disk.isEmpty()) {
            boolean expired = hit == null || now > hit.expireAt();
            if (expired && refreshing.add(key)) {
                final String k = key;
                Thread t = new Thread(() -> {
                    try {
                        List<DayData> fresh = fetchWithRetry(k);
                        saveDisk(k, fresh);
                        cache.put(k, new CacheEntry(System.currentTimeMillis() + Duration.ofHours(3).toMillis(), fresh));
                    } catch (Exception ignored) {
                        // 刷新失败：保留磁盘数据，10 分钟后再试
                    } finally {
                        refreshing.remove(k);
                    }
                });
                t.setDaemon(true);
                t.start();
            }
            cache.put(key, new CacheEntry(now + Duration.ofMinutes(5).toMillis(), disk));
            return Result.success(disk);
        }

        // 冷启动且无任何缓存：同步抓取一次
        try {
            List<DayData> days = fetchWithRetry(key);
            saveDisk(key, days);
            cache.put(key, new CacheEntry(now + Duration.ofHours(3).toMillis(), days));
            return Result.success(days);
        } catch (Exception e) {
            cache.put(key, new CacheEntry(now + Duration.ofMinutes(10).toMillis(), List.of()));
            return Result.error(502, "Failed to load GitHub contributions");
        }
    }

    /** 连试 3 次，间隔递增——服务器到 GitHub 的链路时好时坏。 */
    private List<DayData> fetchWithRetry(String key) throws Exception {
        IllegalStateException last = null;
        for (int i = 0; i < 3; i++) {
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
                return days;
            } catch (Exception e) {
                last = new IllegalStateException(e.getMessage(), e);
                try { Thread.sleep(1500L * (i + 1)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        throw last;
    }

    /** 磁盘缓存（随 jar 工作目录），格式 [{"date":"..","count":n,"level":n}, ..] */
    private java.nio.file.Path diskFile(String key) {
        return java.nio.file.Paths.get("github-cache-" + key + ".json");
    }

    private void saveDisk(String key, List<DayData> days) {
        try {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < days.size(); i++) {
                DayData d = days.get(i);
                if (i > 0) sb.append(',');
                sb.append("{\"date\":\"").append(d.date())
                        .append("\",\"count\":").append(d.count())
                        .append(",\"level\":").append(d.level()).append('}');
            }
            sb.append(']');
            java.nio.file.Files.writeString(diskFile(key), sb.toString());
        } catch (Exception ignored) {}
    }

    private List<DayData> loadDisk(String key) {
        try {
            String s = java.nio.file.Files.readString(diskFile(key));
            List<DayData> days = new ArrayList<>();
            Matcher m = Pattern.compile("\\{\"date\":\"([\\d-]+)\",\"count\":(\\d+),\"level\":(\\d+)\\}").matcher(s);
            while (m.find()) {
                days.add(new DayData(m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))));
            }
            return days;
        } catch (Exception e) {
            return List.of();
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

    /**
     * 力扣活跃度（真实提交记录）：调 leetcode.cn 的 noj-go GraphQL 接口
     * userCalendar(userSlug, year) 拿官方 submissionCalendar（时间戳→次数），
     * 按亚洲/上海时区折算日期，合并当前年与上一年；接口失败时回退到打卡日志 lc-log。
     */
    @GetMapping("/leetcode")
    public Result<List<DayData>> leetcode(
            @RequestParam(value = "user", defaultValue = "elo9uent-turing29l") String user) {
        String slug = user.replaceAll("[^A-Za-z0-9-]", "");
        int year = LocalDate.now().getYear();
        ZoneId cn = ZoneId.of("Asia/Shanghai");

        Map<String, Integer> perDay = new HashMap<>();
        boolean any = false;
        for (int y : new int[]{year, year - 1}) {
            try {
                String payload = "{\"query\":\"query userProfileCalendar($userSlug: String!, $year: Int) { "
                        + "userCalendar(userSlug: $userSlug, year: $year) { streak totalActiveDays submissionCalendar activeYears } }\","
                        + "\"variables\":{\"userSlug\":\"" + slug + "\",\"year\":" + y + "}}";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://leetcode.cn/graphql/noj-go/"))
                        .header("Content-Type", "application/json")
                        .header("Referer", "https://leetcode.cn/u/" + slug + "/")
                        .timeout(Duration.ofSeconds(12))
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) throw new IllegalStateException("lc HTTP " + resp.statusCode());

                Matcher cal = Pattern.compile("\"submissionCalendar\":\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(resp.body());
                if (!cal.find()) throw new IllegalStateException("no submissionCalendar");
                String calJson = cal.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
                Matcher kv = Pattern.compile("\"(\\d{10})\":(\\d+)").matcher(calJson);
                while (kv.find()) {
                    String date = LocalDate.ofInstant(Instant.ofEpochSecond(Long.parseLong(kv.group(1))), cn).toString();
                    int n = Integer.parseInt(kv.group(2));
                    if (n > 0) perDay.merge(date, n, Math::max);
                }
                any = true;
            } catch (Exception ignored) {}
        }

        // 官方接口不可用时，回退到本站手动打卡日志（lc-log）
        if (!any) {
            for (UserProgress row : progressMapper.selectList(new LambdaQueryWrapper<UserProgress>()
                    .eq(UserProgress::getProgKey, "lc-log"))) {
                String v = row.getValue();
                if (v == null || v.isBlank()) continue;
                Matcher mm = Pattern.compile("\"date\"\\s*:\\s*\"(\\d{4}-\\d{2}-\\d{2})\"").matcher(v);
                while (mm.find()) perDay.merge(mm.group(1), 1, Integer::sum);
            }
        }

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(364);
        start = start.minusDays(start.getDayOfWeek().getValue() % 7); // 对齐周日
        List<DayData> days = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            String ds = d.toString();
            int c = perDay.getOrDefault(ds, 0);
            days.add(new DayData(ds, c, levelOf(c)));
        }
        return Result.success(days);
    }
}
