package com.xiaolai.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TagDTO {
    @NotBlank(message = "Tag name is required")
    private String name;

    private String slug;
}