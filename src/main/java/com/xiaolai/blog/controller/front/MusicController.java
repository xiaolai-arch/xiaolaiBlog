package com.xiaolai.blog.controller.front;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.Music;
import com.xiaolai.blog.mapper.MusicMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/music")
public class MusicController {

    private final MusicMapper musicMapper;

    public MusicController(MusicMapper musicMapper) {
        this.musicMapper = musicMapper;
    }

    /// 首页"我喜欢的音乐"卡：最新上传的一首
    @GetMapping("/current")
    public Result<Music> current() {
        return Result.success(musicMapper.selectOne(
                new LambdaQueryWrapper<Music>().orderByDesc(Music::getId).last("LIMIT 1")));
    }
}
