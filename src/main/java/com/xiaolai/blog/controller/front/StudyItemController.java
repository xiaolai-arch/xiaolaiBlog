package com.xiaolai.blog.controller.front;

import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.common.exception.BlogException;
import com.xiaolai.blog.mapper.UserMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/study-items")
public class StudyItemController {
    private final JdbcTemplate db;
    private final UserMapper users;
    public StudyItemController(JdbcTemplate db,UserMapper users) { this.db=db; this.users=users; }
    public record Item(@NotBlank @Size(max=200) String title,@Size(max=500) String sourceUrl,
                       @NotBlank @Pattern(regexp="todo|learning|mastered|review") String state,Long noteId,LocalDateTime reviewAt) {}
    private Long owner(Authentication auth) { return users.selectByUsername(auth.getName()).getId(); }
    private void validate(Item item,long owner) {
        if(item.sourceUrl()!=null && (item.sourceUrl().contains("\\") || item.sourceUrl().chars().anyMatch(Character::isWhitespace))) throw new BlogException(422,"学习链接不能包含空格或反斜杠");
        if(item.sourceUrl()!=null && !item.sourceUrl().isBlank() && !item.sourceUrl().matches("https?://[^\\s]+|/[^/].*")) throw new BlogException(422,"学习链接应为站内路径或 HTTP(S) 地址");
        if(item.noteId()!=null && db.queryForObject("SELECT COUNT(*) FROM note WHERE id=? AND user_id=? AND deleted=0",Integer.class,item.noteId(),owner)!=1) throw new BlogException(422,"只能关联自己的有效笔记");
    }
    @GetMapping public Result<?> list(Authentication auth) {
        return Result.success(db.queryForList("SELECT id,title,source_url AS sourceUrl,state,note_id AS noteId,review_at AS reviewAt FROM study_item WHERE user_id=? ORDER BY update_time DESC,id DESC",owner(auth)));
    }
    @PostMapping public Result<?> create(@Valid @RequestBody Item item,Authentication auth) {
        long owner=owner(auth); validate(item,owner);
        if(db.queryForObject("SELECT COUNT(*) FROM study_item WHERE user_id=?",Integer.class,owner)>=1000) throw new BlogException(422,"最多保留 1000 项学习计划，请先整理");
        db.update("INSERT INTO study_item(user_id,title,source_url,state,note_id,review_at) VALUES(?,?,?,?,?,?)",owner,item.title().trim(),item.sourceUrl()==null?"":item.sourceUrl(),item.state(),item.noteId(),item.reviewAt());
        return list(auth);
    }
    @PutMapping("/{id}") public Result<?> update(@PathVariable Long id,@Valid @RequestBody Item item,Authentication auth) {
        long owner=owner(auth); validate(item,owner);
        if(db.update("UPDATE study_item SET title=?,source_url=?,state=?,note_id=?,review_at=?,update_time=CURRENT_TIMESTAMP WHERE id=? AND user_id=?",item.title().trim(),item.sourceUrl()==null?"":item.sourceUrl(),item.state(),item.noteId(),item.reviewAt(),id,owner)!=1) throw new BlogException(404,"学习项不存在");
        return list(auth);
    }
    @DeleteMapping("/{id}") public Result<?> delete(@PathVariable Long id,Authentication auth) {
        if(db.update("DELETE FROM study_item WHERE id=? AND user_id=?",id,owner(auth))!=1) throw new BlogException(404,"学习项不存在");
        return Result.success();
    }
}
