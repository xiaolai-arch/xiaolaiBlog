package com.xiaolai.blog.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaolai.blog.common.response.PageResult;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.Comment;
import com.xiaolai.blog.service.CommentService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController("adminCommentController")
@RequestMapping("/api/v1/admin/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public Result<PageResult<Comment>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        Page<Comment> result = commentService.getAdminComments(page, size, status);
        return Result.success(PageResult.of(result.getTotal(), page, size, result.getRecords()));
    }

    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        commentService.approve(id);
        return Result.success();
    }

    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id) {
        commentService.reject(id);
        return Result.success();
    }

    @PutMapping("/{id}/spam")
    public Result<Void> markSpam(@PathVariable Long id) {
        commentService.markSpam(id);
        return Result.success();
    }

    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        commentService.reply(id, body.get("content"));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.success();
    }
}