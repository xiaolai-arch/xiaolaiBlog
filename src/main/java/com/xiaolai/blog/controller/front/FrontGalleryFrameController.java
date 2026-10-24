package com.xiaolai.blog.controller.front;

import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.GalleryFrame;
import com.xiaolai.blog.mapper.GalleryFrameMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 画廊画框映射公开读取：3D 展厅按 frame_id 取后台配置的挂画，
 * 没有配置时返回空对象，前台沿用自动填充。
 */
@RestController("frontGalleryFrameController")
public class FrontGalleryFrameController {

    private final GalleryFrameMapper galleryFrameMapper;

    public FrontGalleryFrameController(GalleryFrameMapper galleryFrameMapper) {
        this.galleryFrameMapper = galleryFrameMapper;
    }

    @GetMapping("/api/v1/gallery-frames")
    public Result<Map<String, GalleryFrame>> frames() {
        Map<String, GalleryFrame> map = new HashMap<>();
        for (GalleryFrame frame : galleryFrameMapper.selectAll()) {
            map.put(frame.getFrameId(), frame);
        }
        return Result.success(map);
    }
}
