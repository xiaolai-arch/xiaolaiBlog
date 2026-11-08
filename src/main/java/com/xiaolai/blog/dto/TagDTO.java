package com.xiaolai.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.xiaolai.blog.common.validation.SlugUtils;

@Data
public class TagDTO {
    @NotBlank(message = "Tag name is required")
    @Size(max = 50)
    private String name;

    @Size(max = 100)
    @Pattern(regexp = SlugUtils.PATTERN, message = "Slug can only contain letters, numbers and single hyphens")
    private String slug;
}
