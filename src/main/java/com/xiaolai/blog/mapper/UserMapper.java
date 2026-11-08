package com.xiaolai.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaolai.blog.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM user WHERE username = #{username}")
    User selectByUsername(String username);

    @Update("UPDATE user SET token_version = token_version + 1 WHERE username = #{username}")
    int revokeTokens(String username);

    @Select("SELECT id FROM user WHERE id = #{id} FOR UPDATE")
    Long lockById(Long id);
}
