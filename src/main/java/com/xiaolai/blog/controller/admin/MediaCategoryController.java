package com.xiaolai.blog.controller.admin;

import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.MediaCategory;
import com.xiaolai.blog.service.MediaCategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminMediaCategoryController")
@RequestMapping("/api/v1/admin/media-categories")
public class MediaCategoryController {

    private final MediaCategoryService mediaCategoryService;

    public MediaCategoryController(MediaCategoryService mediaCategoryService) {
        this.mediaCategoryService = mediaCategoryService;
    }

    @GetMapping
    public Result<List<MediaCategory>> list() {
        return Result.success(mediaCategoryService.getAll());
    }

    @GetMapping("/{id}")
    public Result<MediaCategory> get(@PathVariable Long id) {
        return Result.success(mediaCategoryService.getById(id));
    }

    @PostMapping
    public Result<MediaCategory> create(@RequestBody MediaCategory category) {
        return Result.success(mediaCategoryService.create(
            category.getName(), category.getSlug(), category.getDescription()));
    }

    @PutMapping("/{id}")
    public Result<MediaCategory> update(@PathVariable Long id, @RequestBody MediaCategory category) {
        return Result.success(mediaCategoryService.update(
            id, category.getName(), category.getSlug(), category.getDescription()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mediaCategoryService.delete(id);
        return Result.success();
    }
}