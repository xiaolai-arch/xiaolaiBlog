package com.xiaolai.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProgressDTO {
    @NotBlank(message = "Value is required")
    @Size(max = 200000, message = "进度内容过大（上限 200KB）")
    private String value;
}
