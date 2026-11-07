package com.xiaolai.blog.controller.front;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.dto.NoteDTO;
import com.xiaolai.blog.dto.NoteSummary;
import com.xiaolai.blog.common.exception.BlogException;
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
    private final com.xiaolai.blog.service.RevisionService revisions;

    public NoteController(NoteMapper noteMapper, UserMapper userMapper, com.xiaolai.blog.service.RevisionService revisions) {
        this.noteMapper = noteMapper;
        this.userMapper = userMapper;
        this.revisions = revisions;
    }

    @GetMapping
    public Result<List<NoteSummary>> list(@RequestParam(required=false) String q, @RequestParam(required=false) String folder) {
        if ((q != null && q.length() > 256) || (folder != null && folder.length() > 100)) throw new BlogException(422, "搜索条件过长");
        Long userId = currentUserId();
        List<Note> notes = noteMapper.selectList(new LambdaQueryWrapper<Note>()
                .select(Note.class, f -> !"content".equals(f.getColumn()))
                .eq(Note::getUserId, userId)
                .eq(folder != null, Note::getFolder, folder)
                .and(q != null && !q.isBlank(), w -> w.like(Note::getTitle, q).or().like(Note::getContent, q))
                .orderByDesc(Note::getUpdateTime));
        return Result.success(notes.stream().map(NoteSummary::from).toList());
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
        note.setFolder(dto.getFolder() == null ? "" : dto.getFolder().trim());
        noteMapper.insert(note);
        note.setContent(null);
        return Result.success(note);
    }

    @PutMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public Result<Note> update(@PathVariable Long id, @Valid @RequestBody NoteDTO dto) {
        Note note = ownedNote(id);
        if (note == null) return Result.notFound("Note not found");
        boolean changed = (dto.getTitle()!=null && !java.util.Objects.equals(note.getTitle(), dto.getTitle()))
                || (dto.getContent()!=null && !java.util.Objects.equals(note.getContent(), dto.getContent()));
        if (changed) revisions.save("note", id, note.getUserId(), note.getTitle(), note.getContent(), null);
        if (dto.getTitle() != null) note.setTitle(dto.getTitle());
        if (dto.getContent() != null) note.setContent(dto.getContent());
        if (dto.getFolder() != null) note.setFolder(dto.getFolder().trim());
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

    @GetMapping("/trash")
    public Result<?> trash() { return Result.success(noteMapper.trash(currentUserId()).stream().map(NoteSummary::from).toList()); }

    @PostMapping("/{id}/restore")
    public Result<?> restore(@PathVariable Long id) {
        if (noteMapper.restore(id,currentUserId()) != 1) return Result.notFound("回收站中没有这篇笔记");
        return Result.success();
    }

    @DeleteMapping("/{id}/permanent")
    @org.springframework.transaction.annotation.Transactional
    public Result<?> purge(@PathVariable Long id) {
        if (noteMapper.purge(id,currentUserId()) != 1) return Result.notFound("回收站中没有这篇笔记");
        revisions.purge("note", id, currentUserId());
        return Result.success();
    }

    @GetMapping("/{id}/revisions")
    public Result<?> revisions(@PathVariable Long id) {
        if (ownedNote(id)==null) return Result.notFound("Note not found");
        return Result.success(revisions.list("note",id,currentUserId()));
    }

    @GetMapping("/{id}/revisions/{revision}")
    public Result<?> revision(@PathVariable Long id,@PathVariable Long revision) {
        if (ownedNote(id)==null) return Result.notFound("Note not found");
        return Result.success(revisions.get("note",id,currentUserId(),revision));
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/{id}/revisions/{revision}/restore")
    public Result<?> restoreRevision(@PathVariable Long id,@PathVariable Long revision) {
        if (ownedNote(id)==null) return Result.notFound("Note not found");
        var snapshot = revisions.get("note",id,currentUserId(),revision);
        NoteDTO dto = new NoteDTO();
        dto.setTitle((String)snapshot.get("title")); dto.setContent((String)snapshot.get("content"));
        return update(id,dto);
    }

    private Long currentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.selectByUsername(username);
        if (user == null) throw new BlogException(401, "请重新登录");
        return user.getId();
    }
}
