package com.xiaolai.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String contentMd;
    private String contentHtml;
    private String coverUrl;
    private Long categoryId;
    private Integer status;
    private Integer isTop;
    private Integer views;
    @JsonIgnore
    private String password;
    private String seoTitle;
    private String seoDescription;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime publishAt;
    private String seriesName;
    private Integer seriesOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String categorySlug;

    @TableField(exist = false)
    private java.util.List<Tag> tags;

    /** Short Markdown prefix selected by lightweight queries to fill missing legacy summaries. */
    @JsonIgnore
    @TableField(exist = false)
    private String summaryFallback;
}
