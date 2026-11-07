package com.xiaolai.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaolai.blog.entity.UserProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserProgressMapper extends BaseMapper<UserProgress> {
    @Insert("INSERT INTO user_progress(user_id, prog_key, `value`) VALUES(#{userId}, #{key}, '[]') " +
            "ON DUPLICATE KEY UPDATE id = id")
    void ensureRow(@Param("userId") Long userId, @Param("key") String key);

    @Select("SELECT * FROM user_progress WHERE user_id = #{userId} AND prog_key = #{key} FOR UPDATE")
    UserProgress lockRow(@Param("userId") Long userId, @Param("key") String key);
}
