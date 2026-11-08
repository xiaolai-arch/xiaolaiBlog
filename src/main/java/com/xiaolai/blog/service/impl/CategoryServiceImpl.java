package com.xiaolai.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaolai.blog.common.exception.BlogException;
import com.xiaolai.blog.common.validation.SlugUtils;
import com.xiaolai.blog.dto.CategoryDTO;
import com.xiaolai.blog.entity.Article;
import com.xiaolai.blog.entity.Category;
import com.xiaolai.blog.mapper.ArticleMapper;
import com.xiaolai.blog.mapper.CategoryMapper;
import com.xiaolai.blog.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper, ArticleMapper articleMapper) {
        this.categoryMapper = categoryMapper;
        this.articleMapper = articleMapper;
    }

    @Override
    public List<Category> getAllWithCount() {
        return categoryMapper.selectAllWithCount();
    }

    @Override
    public Category getById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) throw new BlogException(404, "Category not found");
        return category;
    }

    @Override
    public Category getBySlug(String slug) {
        return categoryMapper.selectBySlug(slug);
    }

    @Override
    public Category create(CategoryDTO dto) {
        String name = dto.getName().trim();
        String slug = resolveSlug(dto.getSlug(), name, null);
        if (categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, name)) > 0) {
            throw new BlogException(422, "Category name already exists");
        }
        if (categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getSlug, slug)) > 0) {
            throw new BlogException(422, "Category slug already exists");
        }
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(blankToNull(dto.getDescription()));
        categoryMapper.insert(category);
        return category;
    }

    @Override
    public Category update(Long id, CategoryDTO dto) {
        Category category = getById(id);
        String name = dto.getName().trim();
        String slug = resolveSlug(dto.getSlug(), name, category.getSlug());
        if (categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, name).ne(Category::getId, id)) > 0) {
            throw new BlogException(422, "Category name already exists");
        }
        if (categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getSlug, slug).ne(Category::getId, id)) > 0) {
            throw new BlogException(422, "Category slug already exists");
        }
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(blankToNull(dto.getDescription()));
        categoryMapper.updateById(category);
        return category;
    }

    @Override
    public void delete(Long id) {
        long count = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getCategoryId, id)
                .eq(Article::getDeleted, 0));
        if (count > 0) {
            throw new BlogException(422, "Cannot delete category with existing articles");
        }
        categoryMapper.deleteById(id);
    }

    private String resolveSlug(String requested, String name, String current) {
        String value;
        if (requested == null && current != null && !current.isBlank()) {
            value = current;
        } else {
            value = requested == null || requested.isBlank() ? SlugUtils.from(name) : requested;
        }
        return SlugUtils.requireValid(value, "Category slug");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
