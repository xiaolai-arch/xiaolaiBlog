package com.xiaolai.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long parentId;
    private String nickname;
    private String email;
    private String website;
    private String contentMd;
    private String contentHtml;
    private Integer status;
    private Integer isAdmin;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** MD5 of the lower-cased email, for Gravatar; lets the public API omit the raw email. */
    @TableField(exist = false)
    private String emailHash;

    @TableField(exist = false)
    private List<Comment> replies;
}