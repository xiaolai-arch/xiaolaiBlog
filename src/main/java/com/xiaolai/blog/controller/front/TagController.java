package com.xiaolai.blog.controller.front;

import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.Tag;
import com.xiaolai.blog.service.TagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("frontTagController")
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public Result<List<Tag>> list() {
        return Result.success(tagService.getAllWithCount());
    }

    @GetMapping("/{slug}")
    public Result<Tag> detail(@PathVariable String slug) {
        Tag tag = tagService.getBySlug(slug);
        if (tag == null) return Result.notFound("Tag not found");
        return Result.success(tag);
    }
}