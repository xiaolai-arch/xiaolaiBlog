package com.xiaolai.blog.controller.front;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.Media;
import com.xiaolai.blog.mapper.MediaMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.nio.file.Paths;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/// 胶片页数据源：与参考站 FilmPage 前端约定的接口形状（photos: FilmPhoto[]），
/// 数据取自媒体库（图库上传的照片），新→旧。
@RestController
public class FilmPhotosController {

    private final MediaMapper mediaMapper;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    public FilmPhotosController(MediaMapper mediaMapper) {
        this.mediaMapper = mediaMapper;
    }

    @GetMapping("/api/film-photos")
    public Map<String, Object> filmPhotos() {
        List<Media> list = mediaMapper.selectList(
                new LambdaQueryWrapper<Media>()
                        .likeRight(Media::getType, "image/")
                        .orderByDesc(Media::getId));
        List<Map<String, Object>> photos = new ArrayList<>();
        for (Media m : list) {
            Map<String, Object> p = new HashMap<>();
            p.put("id", String.valueOf(m.getId()));
            p.put("src", m.getUrl());
            p.put("title", m.getTitle() == null ? "" : m.getTitle());
            String[] my = monthYear(m.getCreateTime());
            p.put("month", my[0]);
            p.put("year", Integer.parseInt(my[1]));
            p.put("aspectRatio", aspectRatioOf(m));
            if (m.getTitle() != null && !m.getTitle().isBlank()) {
                p.put("note", m.getTitle());
            }
            photos.add(p);
        }
        return Map.of("photos", photos);
    }

    private String[] monthYear(java.time.LocalDateTime createTime) {
        if (createTime == null) return new String[]{"", "2026"};
        return new String[]{
                Month.of(createTime.getMonthValue()).getDisplayName(
                        java.time.format.TextStyle.FULL, Locale.ENGLISH),
                String.valueOf(createTime.getYear())};
    }

    /// 读上传文件的真实宽高（读失败回退 3:2）
    private double aspectRatioOf(Media m) {
        if (m.getUrl() == null || !m.getUrl().startsWith("/uploads/")) return 1.5;
        try (ImageInputStream in = ImageIO.createImageInputStream(
                Paths.get(uploadPath).toAbsolutePath().normalize()
                        .resolve(m.getUrl().substring("/uploads/".length())).toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader r = readers.next();
                try {
                    r.setInput(in);
                    int w = r.getWidth(0), h = r.getHeight(0);
                    if (w > 0 && h > 0) return (double) w / h;
                } finally {
                    r.dispose();
                }
            }
        } catch (Exception ignored) {}
        return 1.5;
    }
}
