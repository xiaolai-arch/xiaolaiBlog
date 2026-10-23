package com.xiaolai.blog.controller.front;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.Anime;
import com.xiaolai.blog.mapper.AnimeMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/anime")
public class AnimeController {

    private final AnimeMapper animeMapper;

    public AnimeController(AnimeMapper animeMapper) {
        this.animeMapper = animeMapper;
    }

    /// 首页动漫小组件：番剧收藏（按 sort_order 升序，新加的 sort 大在后）
    @GetMapping("/list")
    public Result<List<Anime>> list() {
        return Result.success(animeMapper.selectList(
                new LambdaQueryWrapper<Anime>()
                        .select(Anime::getId, Anime::getTitle, Anime::getCoverUrl, Anime::getStatus,
                                Anime::getRating, Anime::getComment)
                        .orderByAsc(Anime::getSortOrder)
                        .orderByDesc(Anime::getId)));
    }
}
