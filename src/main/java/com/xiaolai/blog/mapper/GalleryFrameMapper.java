package com.xiaolai.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaolai.blog.entity.GalleryFrame;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GalleryFrameMapper extends BaseMapper<GalleryFrame> {

    @Select("SELECT * FROM gallery_frame ORDER BY id")
    List<GalleryFrame> selectAll();
}
