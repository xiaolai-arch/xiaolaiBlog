package com.xiaolai.blog.dto;

import com.xiaolai.blog.entity.Article;
import com.xiaolai.blog.entity.Tag;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** Public article metadata used by lists, navigation and archives. */
@Data
public class ArticleSummaryDTO {
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String coverUrl;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private Integer status;
    private Integer isTop;
    private Integer views;
    private LocalDateTime publishAt;
    private String seriesName;
    private Integer seriesOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<Tag> tags;

    public static ArticleSummaryDTO from(Article article) {
        ArticleSummaryDTO dto = new ArticleSummaryDTO();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setSlug(article.getSlug());
        dto.setSummary(article.getSummary());
        dto.setCoverUrl(article.getCoverUrl());
        dto.setCategoryId(article.getCategoryId());
        dto.setCategoryName(article.getCategoryName());
        dto.setCategorySlug(article.getCategorySlug());
        dto.setStatus(article.getStatus());
        dto.setIsTop(article.getIsTop());
        dto.setViews(article.getViews());
        dto.setPublishAt(article.getPublishAt());
        dto.setSeriesName(article.getSeriesName());
        dto.setSeriesOrder(article.getSeriesOrder());
        dto.setCreateTime(article.getCreateTime());
        dto.setUpdateTime(article.getUpdateTime());
        dto.setTags(article.getTags());
        return dto;
    }
}
