package com.xiaolai.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;
import java.time.LocalDateTime;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import com.xiaolai.blog.common.validation.SlugUtils;

@Data
public class ArticleDTO {
    @NotBlank(message = "Title is required")
    @Size(max=200)
    private String title;

    @Size(max = 200)
    @Pattern(regexp = SlugUtils.PATTERN, message = "Slug can only contain letters, numbers and single hyphens")
    private String slug;

    @Size(max = 500)
    private String summary;

    @NotBlank(message = "Content is required")
    @Size(max=1000000)
    private String contentMd;

    @Size(max = 500)
    private String coverUrl;

    private Long categoryId;

    @Min(0)
    @Max(1)
    private Integer status;

    @Min(0)
    @Max(1)
    private Integer isTop;

    @Size(max = 200)
    private String seoTitle;

    @Size(max = 500)
    private String seoDescription;

    @Size(max = 20)
    private List<@NotNull(message = "Tag ID is required") Long> tagIds;

    private LocalDateTime publishAt;
    @Size(max = 100)
    private String seriesName;
    @Min(0)
    @Max(100000)
    private Integer seriesOrder;
}
