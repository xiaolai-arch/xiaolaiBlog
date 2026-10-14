package com.xiaolai.blog.controller.front;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.dto.NoteDTO;
import com.xiaolai.blog.entity.Note;
import com.xiaolai.blog.entity.User;
import com.xiaolai.blog.mapper.NoteMapper;
import com.xiaolai.blog.mapper.UserMapper;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 云端笔记：登录用户（目前即博主本人）的私人笔记 CRUD。
 * 列表接口不返回 content，正文按需通过详情接口拉取。
 */
@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

    private final NoteMapper noteMapper;
    private final UserMapper userMapper;

    public NoteController(NoteMapper noteMapper, UserMapper userMapper) {
        this.noteMapper = noteMapper;
        this.userMapper = userMapper;
    }

    @GetMapping
    public Result<List<Note>> list() {
        Long userId = currentUserId();
        List<Note> notes = noteMapper.selectList(new LambdaQueryWrapper<Note>()
                .select(Note.class, f -> !"content".equals(f.getColumn()))
                .eq(Note::getUserId, userId)
                .orderByDesc(Note::getUpdateTime));
        return Result.success(notes);
    }

    @GetMapping("/{id}")
    public Result<Note> detail(@PathVariable Long id) {
        Note note = ownedNote(id);
        if (note == null) return Result.notFound("Note not found");
        return Result.success(note);
    }

    @PostMapping
    public Result<Note> create(@Valid @RequestBody NoteDTO dto) {
        Note note = new Note();
        note.setUserId(currentUserId());
        note.setTitle(dto.getTitle() == null ? "" : dto.getTitle());
        note.setContent(dto.getContent() == null ? "" : dto.getContent());
        noteMapper.insert(note);
        note.setContent(null);
        return Result.success(note);
    }

    @PutMapping("/{id}")
    public Result<Note> update(@PathVariable Long id, @Valid @RequestBody NoteDTO dto) {
        Note note = ownedNote(id);
        if (note == null) return Result.notFound("Note not found");
        if (dto.getTitle() != null) note.setTitle(dto.getTitle());
        if (dto.getContent() != null) note.setContent(dto.getContent());
        noteMapper.updateById(note);
        note.setContent(null);
        return Result.success(note);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Note note = ownedNote(id);
        if (note == null) return Result.notFound("Note not found");
        noteMapper.deleteById(id);
        return Result.success();
    }

    private Note ownedNote(Long id) {
        Note note = noteMapper.selectById(id);
        if (note == null || !note.getUserId().equals(currentUserId())) return null;
        return note;
    }

    private Long currentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.selectByUsername(username);
        return user == null ? -1L : user.getId();
    }
}
