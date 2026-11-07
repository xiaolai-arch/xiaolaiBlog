package com.xiaolai.blog.dto;

import com.xiaolai.blog.entity.Note;
import java.time.LocalDateTime;

public record NoteSummary(Long id, String title, LocalDateTime createTime, LocalDateTime updateTime) {
    public static NoteSummary from(Note note) {
        return new NoteSummary(note.getId(), note.getTitle(), note.getCreateTime(), note.getUpdateTime());
    }
}
