package com.xiaolai.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.xiaolai.blog.common.validation.SlugUtils;

@Data
public class CategoryDTO {
    @NotBlank(message = "Category name is required")
    @Size(max = 50)
    private String name;

    @Size(max = 100)
    @Pattern(regexp = SlugUtils.PATTERN, message = "Slug can only contain letters, numbers and single hyphens")
    private String slug;

    @Size(max = 200)
    private String description;
}
