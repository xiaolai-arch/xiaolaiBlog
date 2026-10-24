package com.xiaolai.blog.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.GalleryFrame;
import com.xiaolai.blog.mapper.GalleryFrameMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 画廊画框映射管理：按画框 id 覆盖式保存（请求里没带的映射会被删除，
 * 即清空某个画框 = 不传它），前台未配置的画框沿用自动填充。
 */
@RestController("adminGalleryFrameController")
@RequestMapping("/api/v1/admin/gallery-frames")
public class GalleryFrameController {

    private final GalleryFrameMapper galleryFrameMapper;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    public GalleryFrameController(GalleryFrameMapper galleryFrameMapper) {
        this.galleryFrameMapper = galleryFrameMapper;
    }

    /**
     * 裁剪结果上传：存到 uploads 但不进 media 表——画框挂画是策展素材，
     * 不应混进媒体库列表与胶片页。返回可直接用于映射的 URL。
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        String name = file.getOriginalFilename();
        String ext = name != null && name.lastIndexOf('.') >= 0
                ? name.substring(name.lastIndexOf('.') + 1).toLowerCase()
                : "jpg";
        if (!List.of("jpg", "jpeg", "png", "webp").contains(ext)) {
            return Result.badRequest("File type not allowed: " + ext);
        }
        try {
            String datePath = LocalDate.now().toString().replace("-", "/");
            String stored = "frame-" + UUID.randomUUID() + "." + ext;
            Path dir = Paths.get(uploadPath).toAbsolutePath().normalize().resolve(datePath);
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(stored).toFile());
            return Result.success("/uploads/" + datePath + "/" + stored);
        } catch (IOException e) {
            return Result.error(500, "Upload failed: " + e.getMessage());
        }
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
