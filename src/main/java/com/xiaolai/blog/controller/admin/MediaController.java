package com.xiaolai.blog.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.Media;
import com.xiaolai.blog.mapper.MediaMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/files")
public class MediaController {

    private final MediaMapper mediaMapper;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Value("${upload.allowed-extensions:jpg,jpeg,png,gif}")
    private String allowedExtensions;

    public MediaController(MediaMapper mediaMapper) {
        this.mediaMapper = mediaMapper;
    }

    @PostMapping("/upload")
    public Result<Media> upload(@RequestParam("file") MultipartFile file,
                                @RequestParam(value = "title", required = false) String title,
                                @RequestParam(value = "categoryId", required = false) Long categoryId) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            return Result.badRequest("Invalid file name");
        }

        String ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        if (!List.of(allowedExtensions.split(",")).contains(ext)) {
            return Result.badRequest("File type not allowed: " + ext);
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.badRequest("File size exceeds 10MB limit");
        }

        try {
            String datePath = LocalDate.now().toString().replace("-", "/");
            String storedName = UUID.randomUUID() + "." + ext;
            Path targetDir = Paths.get(uploadPath).toAbsolutePath().normalize().resolve(datePath);
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(storedName);
            file.transferTo(targetPath.toFile());

            Media media = new Media();
            media.setOriginalName(originalName);
            media.setTitle(title);
            media.setCategoryId(categoryId);
            media.setStoredName(storedName);
            media.setUrl("/uploads/" + datePath + "/" + storedName);
            media.setPath(targetPath.toString());
            media.setType(file.getContentType());
            media.setSize(file.getSize());
            mediaMapper.insert(media);

            return Result.success(media);
        } catch (IOException e) {
            return Result.error(500, "File upload failed: " + e.getMessage());
        }
    }

    @GetMapping
    public Result<List<Media>> list(@RequestParam(value = "categoryId", required = false) Long categoryId) {
        LambdaQueryWrapper<Media> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Media::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Media::getCreateTime);
        return Result.success(mediaMapper.selectList(wrapper));
    }

    @PutMapping("/{id}")
    public Result<Media> update(@PathVariable Long id, @RequestBody Media update) {
        Media media = mediaMapper.selectById(id);
        if (media == null) return Result.error(404, "File not found");
        media.setTitle(update.getTitle());
        media.setCategoryId(update.getCategoryId());
        mediaMapper.updateById(media);
        return Result.success(media);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Media media = mediaMapper.selectById(id);
        if (media != null) {
            try { Files.deleteIfExists(Paths.get(media.getPath())); } catch (IOException ignored) {}
            mediaMapper.deleteById(id);
        }
        return Result.success();
    }
}