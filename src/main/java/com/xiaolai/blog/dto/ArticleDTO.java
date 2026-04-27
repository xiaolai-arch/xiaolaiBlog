package com.xiaolai.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class ArticleDTO {
    @NotBlank(message = "Title is required")
    private String title;

    private String slug;

    private String summary;

    @NotBlank(message = "Content is required")
    private String contentMd;

    private String coverUrl;

    private Long categoryId;

    private Integer status;

    private Integer isTop;

    private String seoTitle;

    private String seoDescription;

    private List<Long> tagIds;
}