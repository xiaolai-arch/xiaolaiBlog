package com.xiaolai.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaolai.blog.entity.Note;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
    @org.apache.ibatis.annotations.Select("SELECT id,user_id,title,folder,create_time,update_time,deleted FROM note WHERE user_id=#{userId} AND deleted=1 ORDER BY update_time DESC")
    java.util.List<Note> trash(Long userId);

    @org.apache.ibatis.annotations.Update("UPDATE note SET deleted=0,update_time=CURRENT_TIMESTAMP WHERE id=#{id} AND user_id=#{owner} AND deleted=1")
    int restore(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("owner") Long owner);

    @org.apache.ibatis.annotations.Delete("DELETE FROM note WHERE id=#{id} AND user_id=#{owner} AND deleted=1")
    int purge(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("owner") Long owner);
}
