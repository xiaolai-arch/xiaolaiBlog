package com.xiaolai.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaolai.blog.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    @Select("SELECT a.*, c.name as category_name, c.slug as category_slug " +
            "FROM article a LEFT JOIN category c ON a.category_id = c.id " +
            "WHERE a.slug = #{slug} AND a.deleted = 0")
    Article selectBySlug(@Param("slug") String slug);

    @Select("SELECT * FROM article WHERE deleted = 0 ORDER BY is_top DESC, create_time DESC")
    List<Article> selectPublished();

    @Select("SELECT id, title FROM article WHERE category_id = #{categoryId} AND status = 1 AND deleted = 0 " +
            "AND create_time < (SELECT create_time FROM article WHERE id = #{articleId}) " +
            "ORDER BY create_time DESC LIMIT 1")
    Article selectPreviousArticle(@Param("articleId") Long articleId, @Param("categoryId") Long categoryId);

    @Select("SELECT id, title FROM article WHERE category_id = #{categoryId} AND status = 1 AND deleted = 0 " +
            "AND create_time > (SELECT create_time FROM article WHERE id = #{articleId}) " +
            "ORDER BY create_time ASC LIMIT 1")
    Article selectNextArticle(@Param("articleId") Long articleId, @Param("categoryId") Long categoryId);

    @Select("SELECT * FROM article WHERE deleted = 0 AND status = 1 AND is_top = 1 ORDER BY create_time DESC")
    List<Article> selectTopArticles();

    @Select("SELECT DISTINCT DATE_FORMAT(create_time, '%Y') as `year` FROM article " +
            "WHERE status = 1 AND deleted = 0 ORDER BY `year` DESC")
    List<String> selectArchiveYears();

    @Select("SELECT a.*, c.name as category_name, c.slug as category_slug " +
            "FROM article a LEFT JOIN category c ON a.category_id = c.id " +
            "WHERE a.status = 1 AND a.deleted = 0 AND DATE_FORMAT(a.create_time, '%Y-%m') = #{yearMonth} " +
            "ORDER BY a.create_time DESC")
    List<Article> selectByYearMonth(@Param("yearMonth") String yearMonth);

    void incrementViews(@Param("id") Long id);

    List<Article> searchArticles(@Param("keyword") String keyword);

    List<Article> selectByCategorySlug(@Param("slug") String slug);

    List<Article> selectByTagSlug(@Param("slug") String slug);

    void physicalDelete(@Param("id") Long id);
}