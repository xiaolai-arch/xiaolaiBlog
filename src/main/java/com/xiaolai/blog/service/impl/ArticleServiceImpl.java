package com.xiaolai.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaolai.blog.common.constant.BlogConstant;
import com.xiaolai.blog.common.exception.BlogException;
import com.xiaolai.blog.dto.ArticleDTO;
import com.xiaolai.blog.entity.Article;
import com.xiaolai.blog.entity.ArticleTag;
import com.xiaolai.blog.mapper.ArticleMapper;
import com.xiaolai.blog.mapper.ArticleTagMapper;
import com.xiaolai.blog.mapper.TagMapper;
import com.xiaolai.blog.service.ArticleService;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final TagMapper tagMapper;
    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

    public ArticleServiceImpl(ArticleMapper articleMapper,
                              ArticleTagMapper articleTagMapper,
                              TagMapper tagMapper) {
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;
        this.tagMapper = tagMapper;
        this.parser = Parser.builder().build();
        this.htmlRenderer = HtmlRenderer.builder().build();
    }

    @Override
    public Page<Article> getPublishedArticles(int page, int size, String category, String tag, String keyword) {
        Page<Article> pageObj = new Page<>(page, size);

        if (keyword != null && !keyword.isBlank()) {
            List<Article> records = articleMapper.searchArticles(keyword);
            List<Article> paginated = records.stream()
                    .skip((long) (page - 1) * size)
                    .limit(size)
                    .toList();
            Page<Article> result = new Page<>(page, size, records.size());
            result.setRecords(paginated);
            result.getRecords().forEach(a -> a.setTags(tagMapper.selectByArticleId(a.getId())));
            return result;
        }

        if (category != null && !category.isBlank()) {
            List<Article> records = articleMapper.selectByCategorySlug(category);
            List<Article> paginated = records.stream()
                    .skip((long) (page - 1) * size)
                    .limit(size)
                    .toList();
            Page<Article> result = new Page<>(page, size, records.size());
            result.setRecords(paginated);
            result.getRecords().forEach(a -> a.setTags(tagMapper.selectByArticleId(a.getId())));
            return result;
        }

        if (tag != null && !tag.isBlank()) {
            List<Article> records = articleMapper.selectByTagSlug(tag);
            List<Article> paginated = records.stream()
                    .skip((long) (page - 1) * size)
                    .limit(size)
                    .toList();
            Page<Article> result = new Page<>(page, size, records.size());
            result.setRecords(paginated);
            result.getRecords().forEach(a -> a.setTags(tagMapper.selectByArticleId(a.getId())));
            return result;
        }

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, BlogConstant.ARTICLE_STATUS_PUBLISHED)
                .eq(Article::getDeleted, 0)
                .orderByDesc(Article::getIsTop)
                .orderByDesc(Article::getCreateTime);

        Page<Article> result = articleMapper.selectPage(pageObj, wrapper);
        result.getRecords().forEach(a -> a.setTags(tagMapper.selectByArticleId(a.getId())));
        return result;
    }

    @Override
    public Page<Article> getAdminArticles(int page, int size, String keyword, Integer status) {
        Page<Article> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getDeleted, 0);

        if (status != null) {
            wrapper.eq(Article::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Article::getTitle, keyword);
        }
        wrapper.orderByDesc(Article::getCreateTime);

        Page<Article> result = articleMapper.selectPage(pageObj, wrapper);
        result.getRecords().forEach(a -> {
            a.setTags(tagMapper.selectByArticleId(a.getId()));
        });
        return result;
    }

    @Override
    public Article getBySlug(String slug) {
        Article article = articleMapper.selectBySlug(slug);
        if (article != null) {
            article.setTags(tagMapper.selectByArticleId(article.getId()));
        }
        return article;
    }

    @Override
    public Article getById(Long id) {
        Article article = articleMapper.selectById(id);
        if (article != null) {
            article.setTags(tagMapper.selectByArticleId(article.getId()));
        }
        return article;
    }

    @Override
    @Transactional
    public Article create(ArticleDTO dto) {
        Article article = new Article();
        article.setTitle(dto.getTitle());
        article.setSlug(dto.getSlug() != null && !dto.getSlug().isBlank() ? dto.getSlug() : toSlug(dto.getTitle()));
        article.setSummary(dto.getSummary());
        article.setContentMd(dto.getContentMd());
        article.setContentHtml(renderHtml(dto.getContentMd()));
        article.setCoverUrl(dto.getCoverUrl());
        article.setCategoryId(dto.getCategoryId());
        article.setStatus(dto.getStatus() != null ? dto.getStatus() : 0);
        article.setIsTop(dto.getIsTop() != null ? dto.getIsTop() : 0);
        article.setSeoTitle(dto.getSeoTitle());
        article.setSeoDescription(dto.getSeoDescription());
        article.setViews(0);

        articleMapper.insert(article);

        if (dto.getTagIds() != null) {
            for (Long tagId : dto.getTagIds()) {
                ArticleTag at = new ArticleTag();
                at.setArticleId(article.getId());
                at.setTagId(tagId);
                articleTagMapper.insert(at);
            }
        }

        return getById(article.getId());
    }

    @Override
    @Transactional
    public Article update(Long id, ArticleDTO dto) {
        Article article = articleMapper.selectById(id);
        if (article == null) throw new BlogException(404, "Article not found");

        article.setTitle(dto.getTitle());
        if (dto.getSlug() != null && !dto.getSlug().isBlank()) article.setSlug(dto.getSlug());
        article.setSummary(dto.getSummary());
        article.setContentMd(dto.getContentMd());
        article.setContentHtml(renderHtml(dto.getContentMd()));
        article.setCoverUrl(dto.getCoverUrl());
        article.setCategoryId(dto.getCategoryId());
        article.setStatus(dto.getStatus() != null ? dto.getStatus() : article.getStatus());
        article.setIsTop(dto.getIsTop() != null ? dto.getIsTop() : article.getIsTop());
        article.setSeoTitle(dto.getSeoTitle());
        article.setSeoDescription(dto.getSeoDescription());

        articleMapper.updateById(article);

        articleTagMapper.deleteByArticleId(id);
        if (dto.getTagIds() != null) {
            for (Long tagId : dto.getTagIds()) {
                ArticleTag at = new ArticleTag();
                at.setArticleId(id);
                at.setTagId(tagId);
                articleTagMapper.insert(at);
            }
        }

        return getById(id);
    }

    @Override
    public void delete(Long id) {
        articleMapper.deleteById(id); // logical delete
    }

    @Override
    public void restore(Long id) {
        Article article = articleMapper.selectById(id);
        if (article != null) {
            article.setDeleted(0);
            articleMapper.updateById(article);
        }
    }

    @Override
    public void deletePermanently(Long id) {
        articleMapper.physicalDelete(id);
    }

    @Override
    public List<Article> getTopArticles() {
        return articleMapper.selectTopArticles();
    }

    @Override
    public List<String> getArchiveYears() {
        return articleMapper.selectArchiveYears();
    }

    @Override
    public List<Article> getByYearMonth(String yearMonth) {
        return articleMapper.selectByYearMonth(yearMonth);
    }

    @Override
    public Article getPreviousArticle(Long articleId, Long categoryId) {
        return articleMapper.selectPreviousArticle(articleId, categoryId);
    }

    @Override
    public Article getNextArticle(Long articleId, Long categoryId) {
        return articleMapper.selectNextArticle(articleId, categoryId);
    }

    @Override
    public void incrementViews(Long id) {
        articleMapper.incrementViews(id);
    }

    private String toSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-|-$", "");
    }

    private String renderHtml(String md) {
        if (md == null) return "";
        Node document = parser.parse(md);
        return htmlRenderer.render(document);
    }
}