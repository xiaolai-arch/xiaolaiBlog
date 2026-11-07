package com.xiaolai.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;
import java.time.LocalDateTime;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

@Data
public class ArticleDTO {
    @NotBlank(message = "Title is required")
    @Size(max=200)
    private String title;

    private String slug;

    private String summary;

    @NotBlank(message = "Content is required")
    @Size(max=1000000)
    private String contentMd;

    private String coverUrl;

    private Long categoryId;

    private Integer status;

    private Integer isTop;

    private String seoTitle;

    private String seoDescription;

    private List<Long> tagIds;

    private LocalDateTime publishAt;
    @Size(max = 100)
    private String seriesName;
    @Min(0)
    private Integer seriesOrder;
}
