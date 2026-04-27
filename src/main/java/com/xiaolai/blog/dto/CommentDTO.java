package com.xiaolai.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentDTO {
    @NotBlank(message = "Nickname is required")
    private String nickname;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String website;

    @NotBlank(message = "Content is required")
    private String contentMd;

    private Long parentId;
}