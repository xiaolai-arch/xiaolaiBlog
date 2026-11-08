package com.xiaolai.blog.controller.front;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaolai.blog.common.response.PageResult;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.common.exception.BlogException;
import com.xiaolai.blog.dto.GalleryImageDTO;
import com.xiaolai.blog.entity.Media;
import com.xiaolai.blog.entity.MediaCategory;
import com.xiaolai.blog.mapper.MediaCategoryMapper;
import com.xiaolai.blog.mapper.MediaMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
    public Result<PageResult<GalleryImageDTO>> images(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BlogException(422, "page 必须为正数，size 需要在 1-100 之间");
        }
        LambdaQueryWrapper<Media> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Media::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Media::getCreateTime);

        Page<Media> result = mediaMapper.selectPage(new Page<>(page, size), wrapper);
        List<GalleryImageDTO> list = result.getRecords().stream().map(GalleryImageDTO::from).toList();
        return Result.success(PageResult.of(result.getTotal(), page, size, list));
    }
}
