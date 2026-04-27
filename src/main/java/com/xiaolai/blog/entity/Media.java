package com.xiaolai.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("media")
public class Media {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String originalName;
    private String title;
    private Long categoryId;
    private String storedName;
    private String url;
    private String path;
    private String type;
    private Long size;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}