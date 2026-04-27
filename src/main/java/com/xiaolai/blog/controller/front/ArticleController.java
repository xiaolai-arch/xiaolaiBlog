package com.xiaolai.blog.controller.front;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaolai.blog.common.response.PageResult;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.Article;
import com.xiaolai.blog.service.ArticleService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController("frontArticleController")
@RequestMapping("/api/v1")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/articles")
    public Result<PageResult<Article>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword) {
        Page<Article> result = articleService.getPublishedArticles(page, size, category, tag, keyword);
        return Result.success(PageResult.of(result.getTotal(), page, size, result.getRecords()));
    }

    @GetMapping("/articles/top")
    public Result<List<Article>> top() {
        return Result.success(articleService.getTopArticles());
    }

    @GetMapping("/articles/{slug}")
    public Result<Article> detail(@PathVariable String slug) {
        Article article = articleService.getBySlug(slug);
        if (article == null) return Result.notFound("Article not found");

        articleService.incrementViews(article.getId());

        Article prev = articleService.getPreviousArticle(article.getId(), article.getCategoryId());
        Article next = articleService.getNextArticle(article.getId(), article.getCategoryId());

        Map<String, Object> data = new HashMap<>();
        data.put("article", article);
        data.put("previous", prev);
        data.put("next", next);

        return Result.success(article);
    }

    @GetMapping("/articles/{slug}/navigation")
    public Result<Map<String, Object>> navigation(@PathVariable String slug) {
        Article article = articleService.getBySlug(slug);
        if (article == null) return Result.notFound("Article not found");

        Article prev = articleService.getPreviousArticle(article.getId(), article.getCategoryId());
        Article next = articleService.getNextArticle(article.getId(), article.getCategoryId());

        Map<String, Object> nav = new HashMap<>();
        nav.put("previous", prev);
        nav.put("next", next);
        return Result.success(nav);
    }

    @GetMapping("/archives")
    public Result<Map<String, Object>> archives() {
        List<String> years = articleService.getArchiveYears();
        Map<String, List<Article>> archives = new LinkedHashMap<>();
        for (String year : years) {
            for (int month = 12; month >= 1; month--) {
                String ym = year + "-" + String.format("%02d", month);
                List<Article> articles = articleService.getByYearMonth(ym);
                if (!articles.isEmpty()) {
                    archives.put(ym, articles);
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("years", years);
        result.put("archives", archives);
        return Result.success(result);
    }
}