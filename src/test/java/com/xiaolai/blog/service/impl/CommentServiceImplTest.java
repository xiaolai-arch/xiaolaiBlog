package com.xiaolai.blog.service.impl;

import com.xiaolai.blog.entity.Comment;
import com.xiaolai.blog.mapper.CommentMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommentServiceImplTest {

    private final CommentMapper mapper = mock(CommentMapper.class);
    private final CommentServiceImpl service = new CommentServiceImpl(mapper);

    @Test
    void rawHtmlInCommentIsEscaped() {
        String html = service.renderHtml("hi <script>alert(1)</script> <img src=x onerror=alert(1)>");

        assertThat(html).doesNotContain("<script").doesNotContain("<img");
        assertThat(html).contains("&lt;script&gt;");
    }

    @Test
    void javascriptLinksAreNeutralised() {
        String html = service.renderHtml("[click](javascript:alert(1))");

        assertThat(html).doesNotContain("javascript:");
    }

    @Test
    void normalMarkdownStillRenders() {
        String html = service.renderHtml("**bold** and [link](https://example.com)");

        assertThat(html).contains("<strong>bold</strong>").contains("href=\"https://example.com\"");
    }

    @Test
    void publicListingHidesEmailAndAttachesReplies() {
        Comment top = comment(1L, "Visitor@Example.com ", "<b>x</b>");
        Comment reply = comment(2L, "admin@xiaolai.com", "thanks");
        when(mapper.selectApprovedByArticleId(10L)).thenReturn(List.of(top));
        when(mapper.selectRepliesByParentId(1L)).thenReturn(List.of(reply));

        List<Comment> result = service.getApprovedByArticleId(10L);

        Comment c = result.get(0);
        assertThat(c.getEmail()).isNull();
        assertThat(c.getContentMd()).isNull();
        assertThat(c.getEmailHash()).isEqualTo(CommentServiceImpl.emailHash("visitor@example.com"));
        assertThat(c.getContentHtml()).doesNotContain("<b>");
        assertThat(c.getReplies()).hasSize(1);
        assertThat(c.getReplies().get(0).getEmail()).isNull();
        assertThat(c.getReplies().get(0).getContentHtml()).contains("thanks");
    }

    @Test
    void emailHashMatchesGravatarFormat() {
        // Reference value from Gravatar's docs: md5("myemailaddress@example.com")
        assertThat(CommentServiceImpl.emailHash(" MyEmailAddress@example.com "))
                .isEqualTo("0bc83cb571cd1c50ba6f3e8a78ef1346");
        assertThat(CommentServiceImpl.emailHash(null)).isNull();
    }

    private static Comment comment(Long id, String email, String md) {
        Comment c = new Comment();
        c.setId(id);
        c.setEmail(email);
        c.setContentMd(md);
        return c;
    }
}
