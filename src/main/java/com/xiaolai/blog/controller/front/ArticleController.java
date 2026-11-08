package com.xiaolai.blog.controller.front;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaolai.blog.common.constant.BlogConstant;
import com.xiaolai.blog.common.response.PageResult;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.dto.ArticleSummaryDTO;
import com.xiaolai.blog.entity.Article;
import com.xiaolai.blog.service.ArticleService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController("frontArticleController")
@RequestMapping("/api/v1")
public class ArticleController {

    private final ArticleService articleService;
    private final com.xiaolai.blog.mapper.ArticleMapper mapper;

    public ArticleController(ArticleService articleService, com.xiaolai.blog.mapper.ArticleMapper mapper) {
        this.mapper = mapper;
        this.articleService = articleService;
    }

    @GetMapping("/articles")
    public Result<PageResult<ArticleSummaryDTO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword) {
        Page<Article> result = articleService.getPublishedArticles(page, size, category, tag, keyword);
        List<ArticleSummaryDTO> list = result.getRecords().stream().map(ArticleSummaryDTO::from).toList();
        return Result.success(PageResult.of(result.getTotal(), page, size, list));
    }

    @GetMapping("/articles/top")
    public Result<List<ArticleSummaryDTO>> top() {
        return Result.success(articleService.getTopArticles().stream().map(ArticleSummaryDTO::from).toList());
    }

    @GetMapping("/articles/{slug}")
    public Result<Article> detail(@PathVariable String slug) {
        Article article = getPublishedBySlug(slug);
        if (article == null) return Result.notFound("Article not found");

        articleService.incrementViews(article.getId());
        return Result.success(article);
    }

    @GetMapping("/articles/{slug}/navigation")
    public Result<Map<String, Object>> navigation(@PathVariable String slug) {
        Article article = getPublishedBySlug(slug);
        if (article == null) return Result.notFound("Article not found");

        Article prev = articleService.getPreviousArticle(article.getId(), article.getCategoryId());
        Article next = articleService.getNextArticle(article.getId(), article.getCategoryId());

        Map<String, Object> nav = new HashMap<>();
        nav.put("previous", prev == null ? null : ArticleSummaryDTO.from(prev));
        nav.put("next", next == null ? null : ArticleSummaryDTO.from(next));
        return Result.success(nav);
    }

    @GetMapping("/series")
    public Result<List<String>> seriesNames() { return Result.success(mapper.selectSeriesNames()); }

    @GetMapping("/series/articles")
    public Result<List<ArticleSummaryDTO>> series(@RequestParam String name) {
        if (name == null || name.length() > 100) return Result.error(422, "系列名称过长");
        return Result.success(mapper.selectSeries(name).stream().map(ArticleSummaryDTO::from).toList());
    }

    @GetMapping("/archives")
    public Result<Map<String, Object>> archives() {
        Map<String, List<ArticleSummaryDTO>> archives = new LinkedHashMap<>();
        articleService.getArchives().forEach((yearMonth, articles) ->
                archives.put(yearMonth, articles.stream().map(ArticleSummaryDTO::from).toList()));
        List<String> years = archives.keySet().stream()
                .map(yearMonth -> yearMonth.substring(0, 4))
                .distinct()
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("years", years);
        result.put("archives", archives);
        return Result.success(result);
    }

    /** Drafts must not be reachable from the public site, even by guessing the slug. */
    private Article getPublishedBySlug(String slug) {
        Article article = articleService.getBySlug(slug);
        if (article == null || !Objects.equals(article.getStatus(), BlogConstant.ARTICLE_STATUS_PUBLISHED) || (article.getPublishAt() != null && article.getPublishAt().isAfter(java.time.LocalDateTime.now()))) {
            return null;
        }
        return article;
    }
}
