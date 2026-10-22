package com.xiaolai.blog.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.Music;
import com.xiaolai.blog.mapper.MusicMapper;
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
@RequestMapping("/api/v1/admin/music")
public class MusicAdminController {

    private static final long MAX_AUDIO_SIZE = 30L * 1024 * 1024;
    private static final long MAX_COVER_SIZE = 10L * 1024 * 1024;
    private static final List<String> AUDIO_EXTS = List.of("mp3", "m4a", "aac", "flac", "wav", "ogg");
    private static final List<String> COVER_EXTS = List.of("jpg", "jpeg", "png", "gif", "webp");

    private final MusicMapper musicMapper;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    public MusicAdminController(MusicMapper musicMapper) {
        this.musicMapper = musicMapper;
    }

    @PostMapping("/upload")
    public Result<Music> upload(@RequestParam("audio") MultipartFile audio,
                                @RequestParam(value = "cover", required = false) MultipartFile cover,
                                @RequestParam("title") String title,
                                @RequestParam(value = "artist", required = false) String artist,
                                @RequestParam(value = "album", required = false) String album,
                                @RequestParam(value = "lyrics", required = false) String lyrics,
                                @RequestParam(value = "duration", required = false) Integer duration) {
        String audioExt = extOf(audio.getOriginalFilename());
        if (audioExt == null || !AUDIO_EXTS.contains(audioExt)) {
            return Result.badRequest("音频格式不支持，仅支持：" + String.join("/", AUDIO_EXTS));
        }
        if (audio.getSize() > MAX_AUDIO_SIZE) {
            return Result.badRequest("音频超过 30MB 限制");
        }

        try {
            String datePath = LocalDate.now().toString().replace("-", "/");
            Music music = new Music();
            music.setTitle(title);
            music.setArtist(artist);
            music.setAlbum(album);
            music.setLyrics(lyrics);
            music.setDuration(duration);
            music.setFileSize(audio.getSize());
            music.setAudioUrl(saveFile(audio, datePath, audioExt));

            if (cover != null && !cover.isEmpty()) {
                String coverExt = extOf(cover.getOriginalFilename());
                if (coverExt != null && COVER_EXTS.contains(coverExt) && cover.getSize() <= MAX_COVER_SIZE) {
                    music.setCoverUrl(saveFile(cover, datePath, coverExt));
                }
            }

            musicMapper.insert(music);
            return Result.success(music);
        } catch (IOException e) {
            return Result.error(500, "音乐上传失败: " + e.getMessage());
        }
    }

    @GetMapping
    public Result<List<Music>> list() {
        return Result.success(musicMapper.selectList(
                new LambdaQueryWrapper<Music>().orderByDesc(Music::getId)));
    }

    @PutMapping("/{id}")
    public Result<Music> update(@PathVariable Long id, @RequestBody Music update) {
        Music music = musicMapper.selectById(id);
        if (music == null) return Result.error(404, "音乐不存在");
        music.setTitle(update.getTitle());
        music.setArtist(update.getArtist());
        music.setAlbum(update.getAlbum());
        music.setLyrics(update.getLyrics());
        musicMapper.updateById(music);
        return Result.success(music);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Music music = musicMapper.selectById(id);
        if (music != null) {
            deleteFileQuietly(music.getAudioUrl());
            deleteFileQuietly(music.getCoverUrl());
            musicMapper.deleteById(id);
        }
        return Result.success();
    }

    private String saveFile(MultipartFile file, String datePath, String ext) throws IOException {
        Path targetDir = Paths.get(uploadPath).toAbsolutePath().normalize().resolve(datePath);
        Files.createDirectories(targetDir);
        String storedName = UUID.randomUUID() + "." + ext;
        Path targetPath = targetDir.resolve(storedName);
        file.transferTo(targetPath.toFile());
        return "/uploads/" + datePath + "/" + storedName;
    }

    private void deleteFileQuietly(String url) {
        if (url == null || !url.startsWith("/uploads/")) return;
        Path p = Paths.get(uploadPath).toAbsolutePath().normalize()
                .resolve(url.substring("/uploads/".length()));
        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
    }

    private String extOf(String name) {
        if (name == null || !name.contains(".")) return null;
        return name.substring(name.lastIndexOf(".") + 1).toLowerCase();
    }
}
