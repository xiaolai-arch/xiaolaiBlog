package com.xiaolai.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaolai.blog.common.exception.BlogException;
import com.xiaolai.blog.entity.Media;
import com.xiaolai.blog.entity.MediaCategory;
import com.xiaolai.blog.mapper.MediaCategoryMapper;
import com.xiaolai.blog.mapper.MediaMapper;
import com.xiaolai.blog.service.MediaCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MediaCategoryServiceImpl implements MediaCategoryService {

    private final MediaCategoryMapper mediaCategoryMapper;
    private final MediaMapper mediaMapper;

    public MediaCategoryServiceImpl(MediaCategoryMapper mediaCategoryMapper, MediaMapper mediaMapper) {
        this.mediaCategoryMapper = mediaCategoryMapper;
        this.mediaMapper = mediaMapper;
    }

    @Override
    public List<MediaCategory> getAll() {
        return mediaCategoryMapper.selectList(null);
    }

    @Override
    public MediaCategory getById(Long id) {
        MediaCategory cat = mediaCategoryMapper.selectById(id);
        if (cat == null) throw new BlogException(404, "Media category not found");
        return cat;
    }

    @Override
    public MediaCategory getBySlug(String slug) {
        return mediaCategoryMapper.selectOne(
            new LambdaQueryWrapper<MediaCategory>().eq(MediaCategory::getSlug, slug));
    }

    @Override
    public MediaCategory create(String name, String slug, String description) {
        if (mediaCategoryMapper.selectCount(
                new LambdaQueryWrapper<MediaCategory>().eq(MediaCategory::getName, name)) > 0) {
            throw new BlogException(422, "Category name already exists");
        }
        MediaCategory cat = new MediaCategory();
        cat.setName(name);
        cat.setSlug(slug != null ? slug : name.toLowerCase().replaceAll("\\s+", "-"));
        cat.setDescription(description);
        mediaCategoryMapper.insert(cat);
        return cat;
    }

    @Override
    public MediaCategory update(Long id, String name, String slug, String description) {
        MediaCategory cat = getById(id);
        cat.setName(name);
        if (slug != null) cat.setSlug(slug);
        cat.setDescription(description);
        mediaCategoryMapper.updateById(cat);
        return cat;
    }

    @Override
    public void delete(Long id) {
        long count = mediaMapper.selectCount(
            new LambdaQueryWrapper<Media>().eq(Media::getCategoryId, id));
        if (count > 0) {
            throw new BlogException(422, "Cannot delete category that contains images");
        }
        mediaCategoryMapper.deleteById(id);
    }
}