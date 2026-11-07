package com.xiaolai.blog.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaolai.blog.common.response.PageResult;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.dto.ArticleDTO;
import com.xiaolai.blog.entity.Article;
import com.xiaolai.blog.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController("adminArticleController")
@RequestMapping("/api/v1/admin/articles")
public class ArticleController {

    private final ArticleService articleService;
    private final com.xiaolai.blog.service.RevisionService revisions;
    private final com.fasterxml.jackson.databind.ObjectMapper json;

    public ArticleController(ArticleService articleService, com.xiaolai.blog.service.RevisionService revisions, com.fasterxml.jackson.databind.ObjectMapper json) {
        this.revisions = revisions;
        this.json = json;
        this.articleService = articleService;
    }

    @GetMapping
    public Result<PageResult<Article>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        Page<Article> result = articleService.getAdminArticles(page, size, keyword, status);
        return Result.success(PageResult.of(result.getTotal(), page, size, result.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<Article> detail(@PathVariable Long id) {
        Article article = articleService.getById(id);
        if (article == null) return Result.notFound("Article not found");
        return Result.success(article);
    }

    @PostMapping
    public Result<Article> create(@Valid @RequestBody ArticleDTO dto) {
        return Result.success(articleService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Article> update(@PathVariable Long id, @Valid @RequestBody ArticleDTO dto) {
        return Result.success(articleService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) {
        articleService.restore(id);
        return Result.success();
    }

    @DeleteMapping("/{id}/permanent")
    public Result<Void> deletePermanent(@PathVariable Long id) {
        articleService.deletePermanently(id);
        return Result.success();
    }

    @GetMapping("/{id}/revisions")
    public Result<?> revisions(@PathVariable Long id) {
        if (articleService.getById(id) == null) return Result.notFound("Article not found");
        return Result.success(revisions.list("article", id, 0));
    }

    @GetMapping("/{id}/revisions/{revision}")
    public Result<?> revision(@PathVariable Long id, @PathVariable Long revision) {
        if (articleService.getById(id) == null) return Result.notFound("Article not found");
        return Result.success(revisions.get("article", id, 0, revision));
    }

    @PostMapping("/{id}/revisions/{revision}/restore")
    public Result<Article> restoreRevision(@PathVariable Long id, @PathVariable Long revision) throws Exception {
        var row = revisions.get("article", id, 0, revision);
        ArticleDTO dto = json.readValue((String) row.get("metadata"), ArticleDTO.class);
        // Restored content remains a draft until the author explicitly publishes it.
        dto.setStatus(0);
        dto.setPublishAt(null);
        return Result.success(articleService.update(id, dto));
    }
}