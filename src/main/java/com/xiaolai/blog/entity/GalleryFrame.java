package com.xiaolai.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 3D 画廊画框映射：frame_id 对应前台 galleryPaintings 的 12 个画框 id
 * （left-1..3 / back-1..3 / right-1..3 / front-1..3），管理员在后台为
 * 每个画框指定一幅图，未配置的画框沿用前台自动填充。
 */
@Data
@TableName("gallery_frame")
public class GalleryFrame {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String frameId;
    private String imageUrl;
    private String label;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
