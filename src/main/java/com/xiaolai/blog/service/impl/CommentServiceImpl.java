package com.xiaolai.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaolai.blog.common.exception.BlogException;
import com.xiaolai.blog.dto.CommentDTO;
import com.xiaolai.blog.entity.Comment;
import com.xiaolai.blog.mapper.CommentMapper;
import com.xiaolai.blog.service.CommentService;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

    public CommentServiceImpl(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
        this.parser = Parser.builder().build();
        this.htmlRenderer = HtmlRenderer.builder().build();
    }

    @Override
    public Comment create(Long articleId, CommentDTO dto) {
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setParentId(dto.getParentId());
        comment.setNickname(dto.getNickname());
        comment.setEmail(dto.getEmail());
        comment.setWebsite(dto.getWebsite());
        comment.setContentMd(dto.getContentMd());
        comment.setContentHtml(renderHtml(dto.getContentMd()));
        comment.setStatus(0); // pending
        comment.setIsAdmin(0);
        commentMapper.insert(comment);
        return comment;
    }

    @Override
    public List<Comment> getApprovedByArticleId(Long articleId) {
        List<Comment> comments = commentMapper.selectApprovedByArticleId(articleId);
        for (Comment comment : comments) {
            comment.setContentHtml(renderHtml(comment.getContentMd()));
            List<Comment> replies = commentMapper.selectRepliesByParentId(comment.getId());
            replies.forEach(r -> r.setContentHtml(renderHtml(r.getContentMd())));
            comment.setContentMd(null); // not needed in response
        }
        return comments;
    }

    @Override
    public Page<Comment> getAdminComments(int page, int size, Integer status) {
        Page<Comment> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Comment::getStatus, status);
        }
        wrapper.orderByDesc(Comment::getCreateTime);
        return commentMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public void approve(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) throw new BlogException(404, "Comment not found");
        comment.setStatus(1);
        commentMapper.updateById(comment);
    }

    @Override
    public void reject(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) throw new BlogException(404, "Comment not found");
        comment.setStatus(2);
        commentMapper.updateById(comment);
    }

    @Override
    public void markSpam(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) throw new BlogException(404, "Comment not found");
        comment.setStatus(3);
        commentMapper.updateById(comment);
    }

    @Override
    public void delete(Long id) {
        commentMapper.deleteById(id);
    }

    @Override
    public void reply(Long commentId, String content) {
        Comment parent = commentMapper.selectById(commentId);
        if (parent == null) throw new BlogException(404, "Comment not found");

        Comment reply = new Comment();
        reply.setArticleId(parent.getArticleId());
        reply.setParentId(commentId);
        reply.setNickname("Admin");
        reply.setEmail("admin@xiaolai.com");
        reply.setContentMd(content);
        reply.setContentHtml(renderHtml(content));
        reply.setStatus(1); // auto approve for admin
        reply.setIsAdmin(1);
        commentMapper.insert(reply);
    }

    private String renderHtml(String md) {
        if (md == null) return "";
        Node document = parser.parse(md);
        return htmlRenderer.render(document);
    }
}