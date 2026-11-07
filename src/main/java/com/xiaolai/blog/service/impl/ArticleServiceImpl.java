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
    private final com.xiaolai.blog.service.RevisionService revisions;
    private final com.fasterxml.jackson.databind.ObjectMapper json;

    public ArticleServiceImpl(ArticleMapper articleMapper,
                              ArticleTagMapper articleTagMapper,
                              TagMapper tagMapper,
                              com.xiaolai.blog.service.RevisionService revisions,
                              com.fasterxml.jackson.databind.ObjectMapper json) {
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;
        this.tagMapper = tagMapper;
        this.revisions = revisions;
        this.json = json;
        this.parser = Parser.builder().build();
        this.htmlRenderer = HtmlRenderer.builder().build();
    }

    @Override
    public Page<Article> getPublishedArticles(int page, int size, String category, String tag, String keyword) {
        validatePagination(page, size);
        if (keyword != null && keyword.length() > 256) throw new BlogException(422, "搜索词过长");
        Page<Article> result = articleMapper.selectPublicPage(new Page<>(page, size), category, tag, keyword);
        attachTags(result.getRecords());
        return result;
    }

    private void validatePagination(int page, int size) {
        if (page < 1 || size < 1 || size > 100) throw new BlogException(422, "page 必须为正数，size 需要在 1-100 之间");
    }

    private void attachTags(List<Article> articles) {
        if (articles.isEmpty()) return;
        var byArticle = tagMapper.selectByArticleIds(articles.stream().map(Article::getId).toList())
                .stream().collect(java.util.stream.Collectors.groupingBy(com.xiaolai.blog.entity.Tag::getArticleId));
        articles.forEach(article -> article.setTags(byArticle.getOrDefault(article.getId(), List.of())));
    }

    @Override
    public Page<Article> getAdminArticles(int page, int size, String keyword, Integer status) {
        validatePagination(page, size);
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
        attachTags(result.getRecords());
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
        applyPublication(article, dto);

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

        try {
            ArticleDTO previous = new ArticleDTO();
            org.springframework.beans.BeanUtils.copyProperties(article, previous);
            previous.setTagIds(tagMapper.selectByArticleId(id).stream().map(com.xiaolai.blog.entity.Tag::getId).toList());
            revisions.save("article", id, 0, article.getTitle(), article.getContentMd(), json.writeValueAsString(previous));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Unable to preserve article revision", e);
        }

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

        applyPublication(article, dto);

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
        if (articleMapper.restoreDeleted(id) != 1) throw new BlogException(404, "Deleted article not found");
    }

    @Override
    @Transactional
    public void deletePermanently(Long id) {
        articleMapper.physicalDelete(id);
        revisions.purge("article", id, 0);
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

    private void applyPublication(Article article, ArticleDTO dto) {
        if (article.getStatus() != 0 && article.getStatus() != 1) throw new BlogException(422, "文章状态无效");
        article.setPublishAt(dto.getPublishAt());
        article.setSeriesName(dto.getSeriesName() == null ? "" : dto.getSeriesName().trim());
        article.setSeriesOrder(dto.getSeriesOrder() == null ? 0 : dto.getSeriesOrder());
    }

    private String renderHtml(String md) {
        if (md == null) return "";
        Node document = parser.parse(md);
        return htmlRenderer.render(document);
    }
}
