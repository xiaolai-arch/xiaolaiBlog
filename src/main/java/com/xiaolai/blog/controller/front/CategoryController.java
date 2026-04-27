package com.xiaolai.blog.controller.front;

import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.Category;
import com.xiaolai.blog.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("frontCategoryController")
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Result<List<Category>> list() {
        return Result.success(categoryService.getAllWithCount());
    }

    @GetMapping("/{slug}")
    public Result<Category> detail(@PathVariable String slug) {
        Category category = categoryService.getBySlug(slug);
        if (category == null) return Result.notFound("Category not found");
        return Result.success(category);
    }
}