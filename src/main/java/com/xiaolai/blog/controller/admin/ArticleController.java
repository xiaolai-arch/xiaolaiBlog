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

    public ArticleController(ArticleService articleService) {
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
}