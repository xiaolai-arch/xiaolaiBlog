package com.xiaolai.blog.service;

import com.xiaolai.blog.entity.MediaCategory;

import java.util.List;

public interface MediaCategoryService {
    List<MediaCategory> getAll();
    MediaCategory getById(Long id);
    MediaCategory getBySlug(String slug);
    MediaCategory create(String name, String slug, String description);
    MediaCategory update(Long id, String name, String slug, String description);
    void delete(Long id);
}