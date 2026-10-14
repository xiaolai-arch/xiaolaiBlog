package com.xiaolai.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentDTO {
    @NotBlank(message = "Nickname is required")
    @Size(max = 50, message = "Nickname must be at most 50 characters")
    private String nickname;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @Size(max = 200, message = "Website must be at most 200 characters")
    @Pattern(regexp = "^$|^https?://.+", message = "Website must start with http:// or https://")
    private String website;

    @NotBlank(message = "Content is required")
    @Size(max = 2000, message = "Content must be at most 2000 characters")
    private String contentMd;

    private Long parentId;
}
