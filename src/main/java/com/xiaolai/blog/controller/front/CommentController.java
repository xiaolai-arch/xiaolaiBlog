package com.xiaolai.blog.controller.front;

import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.dto.CommentDTO;
import com.xiaolai.blog.entity.Article;
import com.xiaolai.blog.entity.Comment;
import com.xiaolai.blog.mapper.ArticleMapper;
import com.xiaolai.blog.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("frontCommentController")
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService commentService;
    private final ArticleMapper articleMapper;

    public CommentController(CommentService commentService, ArticleMapper articleMapper) {
        this.commentService = commentService;
        this.articleMapper = articleMapper;
    }

    @GetMapping("/articles/{slug}/comments")
    public Result<List<Comment>> list(@PathVariable String slug) {
        Article article = articleMapper.selectBySlug(slug);
        if (article == null) return Result.notFound("Article not found");
        return Result.success(commentService.getApprovedByArticleId(article.getId()));
    }

    @PostMapping("/articles/{slug}/comments")
    public Result<Comment> create(@PathVariable String slug,
                                  @Valid @RequestBody CommentDTO dto) {
        Article article = articleMapper.selectBySlug(slug);
        if (article == null) return Result.notFound("Article not found");
        Comment comment = commentService.create(article.getId(), dto);
        return Result.success(comment);
    }
}