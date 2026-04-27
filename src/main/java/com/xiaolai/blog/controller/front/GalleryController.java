package com.xiaolai.blog.controller.front;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaolai.blog.common.response.PageResult;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.Media;
import com.xiaolai.blog.entity.MediaCategory;
import com.xiaolai.blog.mapper.MediaCategoryMapper;
import com.xiaolai.blog.mapper.MediaMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController("frontGalleryController")
@RequestMapping("/api/v1/gallery")
public class GalleryController {

    private final MediaCategoryMapper mediaCategoryMapper;
    private final MediaMapper mediaMapper;

    public GalleryController(MediaCategoryMapper mediaCategoryMapper, MediaMapper mediaMapper) {
        this.mediaCategoryMapper = mediaCategoryMapper;
        this.mediaMapper = mediaMapper;
    }

    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> categories() {
        List<MediaCategory> categories = mediaCategoryMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (MediaCategory cat : categories) {
            long count = mediaMapper.selectCount(
                new LambdaQueryWrapper<Media>().eq(Media::getCategoryId, cat.getId()));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", cat.getId());
            item.put("name", cat.getName());
            item.put("slug", cat.getSlug());
            item.put("count", count);
            result.add(item);
        }
        return Result.success(result);
    }

    @GetMapping("/images")
    public Result<PageResult<Media>> images(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        LambdaQueryWrapper<Media> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Media::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Media::getCreateTime);

        long total = mediaMapper.selectCount(wrapper);
        int offset = (page - 1) * size;
        wrapper.last("LIMIT " + offset + "," + size);

        List<Media> list = mediaMapper.selectList(wrapper);
        return Result.success(PageResult.of(total, page, size, list));
    }
}