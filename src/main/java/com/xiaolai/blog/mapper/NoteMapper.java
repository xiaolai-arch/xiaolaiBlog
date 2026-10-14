package com.xiaolai.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaolai.blog.entity.Note;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
}
