package com.xiaolai.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaolai.blog.common.exception.BlogException;
import com.xiaolai.blog.common.validation.SlugUtils;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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
        fillSummaryFallbacks(result.getRecords());
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

    private void fillSummaryFallbacks(List<Article> articles) {
        articles.forEach(article -> {
            if (article.getSummary() == null || article.getSummary().isBlank()) {
                article.setSummary(makeSummary(null, article.getSummaryFallback()));
            }
            article.setSummaryFallback(null);
        });
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
        String slug = dto.getSlug() != null && !dto.getSlug().isBlank()
                ? SlugUtils.requireValid(dto.getSlug(), "文章 slug")
                : SlugUtils.from(dto.getTitle());
        SlugUtils.requireValid(slug, "文章 slug");
        if (articleMapper.countBySlugIncludingDeleted(slug, null) > 0) {
            throw new BlogException(422, "文章 slug 已存在");
        }
        article.setSlug(slug);
        article.setSummary(makeSummary(dto.getSummary(), dto.getContentMd()));
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
        attachRequestedTags(article.getId(), dto.getTagIds());

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
        if (dto.getSlug() != null && !dto.getSlug().isBlank()) {
            String slug = SlugUtils.requireValid(dto.getSlug(), "文章 slug");
            if (articleMapper.countBySlugIncludingDeleted(slug, id) > 0) {
                throw new BlogException(422, "文章 slug 已存在");
            }
            article.setSlug(slug);
        }
        article.setSummary(makeSummary(dto.getSummary(), dto.getContentMd()));
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
        attachRequestedTags(id, dto.getTagIds());

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
        List<Article> articles = articleMapper.selectTopArticles();
        fillSummaryFallbacks(articles);
        return articles;
    }

    @Override
    public Map<String, List<Article>> getArchives() {
        List<Article> articles = articleMapper.selectArchiveEntries();
        fillSummaryFallbacks(articles);
        Map<String, List<Article>> archives = new LinkedHashMap<>();
        for (Article article : articles) {
            if (article.getCreateTime() == null) continue;
            String yearMonth = article.getCreateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            archives.computeIfAbsent(yearMonth, ignored -> new java.util.ArrayList<>()).add(article);
        }
        return archives;
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

    private void attachRequestedTags(Long articleId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return;
        List<Long> unique = tagIds.stream().filter(Objects::nonNull).distinct().toList();
        if (unique.size() != tagIds.size() || unique.size() > 20) {
            throw new BlogException(422, "标签数量无效或超过 20 个");
        }
        if (tagMapper.selectBatchIds(unique).size() != unique.size()) {
            throw new BlogException(422, "包含不存在的标签");
        }
        for (Long tagId : unique) {
            ArticleTag at = new ArticleTag();
            at.setArticleId(articleId);
            at.setTagId(tagId);
            articleTagMapper.insert(at);
        }
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

    static String makeSummary(String explicit, String markdown) {
        String value = explicit == null ? "" : explicit.trim();
        if (value.isEmpty() && markdown != null) {
            value = markdown
                    .replaceAll("(?s)```.*?```", " ")
                    .replaceAll("!\\[([^]]*)]\\([^)]*\\)", "$1")
                    .replaceAll("\\[([^]]+)]\\([^)]*\\)", "$1")
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("(?m)^\\s{0,3}(?:#{1,6}|>|[-+*]|\\d+[.)])\\s*", " ")
                    .replaceAll("[`*_~|]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
        }
        if (value.length() > 280) return value.substring(0, 279).trim() + "…";
        return value;
    }
}
