package com.xiaolai.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaolai.blog.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    @Select("SELECT c.*, COUNT(a.id) as article_count " +
            "FROM category c LEFT JOIN article a ON c.id = a.category_id AND a.status = 1 AND a.deleted = 0 " +
            "GROUP BY c.id ORDER BY article_count DESC")
    java.util.List<Category> selectAllWithCount();

    @Select("SELECT c.* FROM category c WHERE c.slug = #{slug}")
    Category selectBySlug(@Param("slug") String slug);
}