package com.xiaolai.blog.controller.admin;

import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.dto.TagDTO;
import com.xiaolai.blog.entity.Tag;
import com.xiaolai.blog.service.TagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminTagController")
@RequestMapping("/api/v1/admin/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public Result<List<Tag>> list() {
        return Result.success(tagService.getAllWithCount());
    }

    @PostMapping
    public Result<Tag> create(@Valid @RequestBody TagDTO dto) {
        return Result.success(tagService.create(dto));
    }

    @GetMapping("/{id}")
    public Result<Tag> detail(@PathVariable Long id) {
        return Result.success(tagService.getById(id));
    }

    @PutMapping("/{id}")
    public Result<Tag> update(@PathVariable Long id, @Valid @RequestBody TagDTO dto) {
        return Result.success(tagService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.success();
    }
}