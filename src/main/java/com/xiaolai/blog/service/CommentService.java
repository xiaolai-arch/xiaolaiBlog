package com.xiaolai.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaolai.blog.dto.CommentDTO;
import com.xiaolai.blog.entity.Comment;

import java.util.List;

public interface CommentService {
    Comment create(Long articleId, CommentDTO dto);
    List<Comment> getApprovedByArticleId(Long articleId);
    Page<Comment> getAdminComments(int page, int size, Integer status);
    void approve(Long id);
    void reject(Long id);
    void markSpam(Long id);
    void delete(Long id);
    void reply(Long commentId, String content);
}