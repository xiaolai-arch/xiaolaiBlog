package com.xiaolai.blog.service;

import com.xiaolai.blog.dto.TagDTO;
import com.xiaolai.blog.entity.Tag;

import java.util.List;

public interface TagService {
    List<Tag> getAllWithCount();
    Tag getById(Long id);
    Tag getBySlug(String slug);
    List<Tag> getByArticleId(Long articleId);
    Tag create(TagDTO dto);
    Tag update(Long id, TagDTO dto);
    void delete(Long id);
}