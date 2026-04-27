package com.xiaolai.blog.service;

import com.xiaolai.blog.dto.CategoryDTO;
import com.xiaolai.blog.entity.Category;

import java.util.List;

public interface CategoryService {
    List<Category> getAllWithCount();
    Category getById(Long id);
    Category getBySlug(String slug);
    Category create(CategoryDTO dto);
    Category update(Long id, CategoryDTO dto);
    void delete(Long id);
}