package com.xiaolai.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaolai.blog.common.exception.BlogException;
import com.xiaolai.blog.common.validation.SlugUtils;
import com.xiaolai.blog.dto.TagDTO;
import com.xiaolai.blog.entity.Tag;
import com.xiaolai.blog.mapper.TagMapper;
import com.xiaolai.blog.service.TagService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    public TagServiceImpl(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    @Override
    public List<Tag> getAllWithCount() {
        return tagMapper.selectAllWithCount();
    }

    @Override
    public Tag getById(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) throw new BlogException(404, "Tag not found");
        return tag;
    }

    @Override
    public Tag getBySlug(String slug) {
        return tagMapper.selectBySlug(slug);
    }

    @Override
    public List<Tag> getByArticleId(Long articleId) {
        return tagMapper.selectByArticleId(articleId);
    }

    @Override
    public Tag create(TagDTO dto) {
        String name = dto.getName().trim();
        String slug = resolveSlug(dto.getSlug(), name, null);
        if (tagMapper.selectCount(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getName, name)) > 0) {
            throw new BlogException(422, "Tag name already exists");
        }
        if (tagMapper.selectCount(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getSlug, slug)) > 0) {
            throw new BlogException(422, "Tag slug already exists");
        }
        Tag tag = new Tag();
        tag.setName(name);
        tag.setSlug(slug);
        tagMapper.insert(tag);
        return tag;
    }

    @Override
    public Tag update(Long id, TagDTO dto) {
        Tag tag = getById(id);
        String name = dto.getName().trim();
        String slug = resolveSlug(dto.getSlug(), name, tag.getSlug());
        if (tagMapper.selectCount(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getName, name).ne(Tag::getId, id)) > 0) {
            throw new BlogException(422, "Tag name already exists");
        }
        if (tagMapper.selectCount(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getSlug, slug).ne(Tag::getId, id)) > 0) {
            throw new BlogException(422, "Tag slug already exists");
        }
        tag.setName(name);
        tag.setSlug(slug);
        tagMapper.updateById(tag);
        return tag;
    }

    @Override
    public void delete(Long id) {
        tagMapper.deleteById(id);
    }

    private String resolveSlug(String requested, String name, String current) {
        String value;
        if (requested == null && current != null && !current.isBlank()) {
            value = current;
        } else {
            value = requested == null || requested.isBlank() ? SlugUtils.from(name) : requested;
        }
        return SlugUtils.requireValid(value, "Tag slug");
    }
}
