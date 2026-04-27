package com.xiaolai.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaolai.blog.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    @Select("SELECT t.*, COUNT(at.article_id) as article_count " +
            "FROM tag t LEFT JOIN article_tag at ON t.id = at.tag_id " +
            "LEFT JOIN article a ON at.article_id = a.id AND a.status = 1 AND a.deleted = 0 " +
            "GROUP BY t.id ORDER BY article_count DESC")
    List<Tag> selectAllWithCount();

    @Select("SELECT t.* FROM tag t JOIN article_tag at ON t.id = at.tag_id WHERE at.article_id = #{articleId}")
    List<Tag> selectByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT t.* FROM tag t WHERE t.slug = #{slug}")
    Tag selectBySlug(@Param("slug") String slug);
}