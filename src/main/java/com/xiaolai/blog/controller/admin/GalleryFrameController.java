package com.xiaolai.blog.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.GalleryFrame;
import com.xiaolai.blog.mapper.GalleryFrameMapper;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 画廊画框映射管理：按画框 id 覆盖式保存（请求里没带的映射会被删除，
 * 即清空某个画框 = 不传它），前台未配置的画框沿用自动填充。
 */
@RestController("adminGalleryFrameController")
@RequestMapping("/api/v1/admin/gallery-frames")
public class GalleryFrameController {

    private final GalleryFrameMapper galleryFrameMapper;

    public GalleryFrameController(GalleryFrameMapper galleryFrameMapper) {
        this.galleryFrameMapper = galleryFrameMapper;
    }

    @GetMapping
    public Result<List<GalleryFrame>> list() {
        return Result.success(galleryFrameMapper.selectAll());
    }

    @PutMapping
    public Result<Void> save(@RequestBody List<GalleryFrame> frames) {
        if (frames == null) {
            return Result.badRequest("Invalid payload");
        }
        Set<String> incomingIds = new HashSet<>();
        for (GalleryFrame frame : frames) {
            if (frame.getFrameId() == null || frame.getFrameId().isBlank()
                    || frame.getImageUrl() == null || frame.getImageUrl().isBlank()) {
                continue;
            }
            incomingIds.add(frame.getFrameId());
            GalleryFrame existing = galleryFrameMapper.selectOne(
                    new LambdaQueryWrapper<GalleryFrame>()
                            .eq(GalleryFrame::getFrameId, frame.getFrameId()));
            if (existing != null) {
                existing.setImageUrl(frame.getImageUrl());
                existing.setLabel(frame.getLabel());
                galleryFrameMapper.updateById(existing);
            } else {
                GalleryFrame next = new GalleryFrame();
                next.setFrameId(frame.getFrameId());
                next.setImageUrl(frame.getImageUrl());
                next.setLabel(frame.getLabel());
                galleryFrameMapper.insert(next);
            }
        }
        // 删除请求里没有的旧映射（清空 = 不传该画框）
        List<GalleryFrame> all = galleryFrameMapper.selectAll();
        for (GalleryFrame old : all) {
            if (!incomingIds.contains(old.getFrameId())) {
                galleryFrameMapper.deleteById(old.getId());
            }
        }
        return Result.success(null);
    }
}
