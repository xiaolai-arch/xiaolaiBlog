package com.xiaolai.blog.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.Anime;
import com.xiaolai.blog.mapper.AnimeMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/anime")
public class AnimeAdminController {

    private final AnimeMapper animeMapper;

    public AnimeAdminController(AnimeMapper animeMapper) {
        this.animeMapper = animeMapper;
    }

    @GetMapping
    public Result<List<Anime>> list() {
        return Result.success(animeMapper.selectList(
                new LambdaQueryWrapper<Anime>().orderByAsc(Anime::getSortOrder).orderByDesc(Anime::getId)));
    }

    @PostMapping
    public Result<Anime> create(@RequestBody Anime anime) {
        if (anime.getTitle() == null || anime.getTitle().isBlank()) {
            return Result.badRequest("番剧名不能为空");
        }
        if (anime.getStatus() == null || anime.getStatus().isBlank()) {
            anime.setStatus("watched");
        }
        if (anime.getSortOrder() == null) {
            anime.setSortOrder(0);
        }
        animeMapper.insert(anime);
        return Result.success(anime);
    }

    @PutMapping("/{id}")
    public Result<Anime> update(@PathVariable Long id, @RequestBody Anime update) {
        Anime anime = animeMapper.selectById(id);
        if (anime == null) return Result.error(404, "番剧不存在");
        anime.setTitle(update.getTitle());
        anime.setCoverUrl(update.getCoverUrl());
        anime.setStatus(update.getStatus());
        anime.setRating(update.getRating());
        anime.setComment(update.getComment());
        anime.setSortOrder(update.getSortOrder());
        animeMapper.updateById(anime);
        return Result.success(anime);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Anime anime = animeMapper.selectById(id);
        if (anime != null) {
            animeMapper.deleteById(id);
        }
        return Result.success();
    }
}
