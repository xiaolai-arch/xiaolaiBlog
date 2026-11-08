package com.xiaolai.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaolai.blog.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    @Select("SELECT a.*, c.name as category_name, c.slug as category_slug " +
            "FROM article a LEFT JOIN category c ON a.category_id = c.id " +
            "WHERE a.slug = #{slug} AND a.deleted = 0")
    Article selectBySlug(@Param("slug") String slug);

    @Select("SELECT id, title, slug, summary, cover_url, category_id, status, is_top, views, publish_at, " +
            "series_name, series_order, create_time, update_time, LEFT(content_md, 500) AS summary_fallback " +
            "FROM article WHERE deleted = 0 AND status = 1 AND (publish_at IS NULL OR publish_at <= CURRENT_TIMESTAMP) " +
            "ORDER BY create_time DESC LIMIT 20")
    List<Article> selectPublished();

    @Select("SELECT id, title, slug FROM article WHERE (category_id = #{categoryId} OR (category_id IS NULL AND #{categoryId} IS NULL)) AND status = 1 AND (publish_at IS NULL OR publish_at <= CURRENT_TIMESTAMP) AND deleted = 0 " +
            "AND (create_time, id) < (SELECT create_time, id FROM article WHERE id = #{articleId}) " +
            "ORDER BY create_time DESC, id DESC LIMIT 1")
    Article selectPreviousArticle(@Param("articleId") Long articleId, @Param("categoryId") Long categoryId);

    @Select("SELECT id, title, slug FROM article WHERE (category_id = #{categoryId} OR (category_id IS NULL AND #{categoryId} IS NULL)) AND status = 1 AND (publish_at IS NULL OR publish_at <= CURRENT_TIMESTAMP) AND deleted = 0 " +
            "AND (create_time, id) > (SELECT create_time, id FROM article WHERE id = #{articleId}) " +
            "ORDER BY create_time ASC, id ASC LIMIT 1")
    Article selectNextArticle(@Param("articleId") Long articleId, @Param("categoryId") Long categoryId);

    @Select("SELECT a.id, a.title, a.slug, a.summary, a.cover_url, a.category_id, a.status, a.is_top, a.views, " +
            "a.publish_at, a.series_name, a.series_order, a.create_time, a.update_time, " +
            "c.name AS category_name, c.slug AS category_slug, LEFT(a.content_md, 500) AS summary_fallback " +
            "FROM article a LEFT JOIN category c ON a.category_id = c.id " +
            "WHERE a.deleted = 0 AND a.status = 1 AND (a.publish_at IS NULL OR a.publish_at <= CURRENT_TIMESTAMP) " +
            "AND a.is_top = 1 ORDER BY a.create_time DESC")
    List<Article> selectTopArticles();

    @Select("SELECT a.id, a.title, a.slug, a.summary, a.cover_url, a.category_id, a.status, a.is_top, a.views, " +
            "a.publish_at, a.series_name, a.series_order, a.create_time, a.update_time, " +
            "c.name as category_name, c.slug as category_slug, LEFT(a.content_md, 500) AS summary_fallback " +
            "FROM article a LEFT JOIN category c ON a.category_id = c.id " +
            "WHERE a.status = 1 AND (a.publish_at IS NULL OR a.publish_at <= CURRENT_TIMESTAMP) AND a.deleted = 0 " +
            "ORDER BY a.create_time DESC")
    List<Article> selectArchiveEntries();

    @Select("SELECT COUNT(*) FROM article WHERE slug = #{slug} AND (#{excludeId} IS NULL OR id <> #{excludeId})")
    long countBySlugIncludingDeleted(@Param("slug") String slug, @Param("excludeId") Long excludeId);

    @Update("UPDATE article SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreDeleted(@Param("id") Long id);

    Page<Article> selectPublicPage(Page<Article> page, @Param("category") String category,
                                   @Param("tag") String tag, @Param("keyword") String keyword);

    @Select("SELECT id,title,slug,series_name,series_order FROM article WHERE deleted=0 AND status=1 " +
            "AND (publish_at IS NULL OR publish_at <= CURRENT_TIMESTAMP) AND series_name=#{name} ORDER BY series_order,id")
    List<Article> selectSeries(@Param("name") String name);

    @Select("SELECT DISTINCT series_name FROM article WHERE deleted=0 AND status=1 " +
            "AND (publish_at IS NULL OR publish_at <= CURRENT_TIMESTAMP) AND series_name<>'' ORDER BY series_name")
    List<String> selectSeriesNames();

    void incrementViews(@Param("id") Long id);

    void physicalDelete(@Param("id") Long id);
}
