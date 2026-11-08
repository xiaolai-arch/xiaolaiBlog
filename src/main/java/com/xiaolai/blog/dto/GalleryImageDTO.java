package com.xiaolai.blog.dto;

import com.xiaolai.blog.entity.Media;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Public gallery representation. Storage paths and generated filenames stay
 * server-side even though the media record itself contains those fields.
 */
@Data
public class GalleryImageDTO {
    private Long id;
    private String originalName;
    private String title;
    private Long categoryId;
    private String url;
    private String type;
    private Long size;
    private LocalDateTime createTime;

    public static GalleryImageDTO from(Media media) {
        GalleryImageDTO dto = new GalleryImageDTO();
        dto.setId(media.getId());
        dto.setOriginalName(media.getOriginalName());
        dto.setTitle(media.getTitle());
        dto.setCategoryId(media.getCategoryId());
        dto.setUrl(media.getUrl());
        dto.setType(media.getType());
        dto.setSize(media.getSize());
        dto.setCreateTime(media.getCreateTime());
        return dto;
    }
}
