package com.xiaolai.blog.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoteDTO {
    @Size(max = 200, message = "标题最多 200 字")
    private String title;

    @Size(max = 200000, message = "内容过长（上限 200KB）")
    private String content;
}
