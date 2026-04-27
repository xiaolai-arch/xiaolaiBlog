package com.xiaolai.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaolai.blog.dto.ArticleDTO;
import com.xiaolai.blog.entity.Article;

import java.util.List;
import java.util.Map;

public interface ArticleService {
    Page<Article> getPublishedArticles(int page, int size, String category, String tag, String keyword);
    Page<Article> getAdminArticles(int page, int size, String keyword, Integer status);
    Article getBySlug(String slug);
    Article getById(Long id);
    Article create(ArticleDTO dto);
    Article update(Long id, ArticleDTO dto);
    void delete(Long id);
    void restore(Long id);
    void deletePermanently(Long id);
    List<Article> getTopArticles();
    List<String> getArchiveYears();
    List<Article> getByYearMonth(String yearMonth);
    Article getPreviousArticle(Long articleId, Long categoryId);
    Article getNextArticle(Long articleId, Long categoryId);
    void incrementViews(Long id);
}