package com.xiaolai.blog.controller.front;

import com.xiaolai.blog.entity.Article;
import com.xiaolai.blog.mapper.ArticleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedControllerTest {

    private final ArticleMapper mapper = mock(ArticleMapper.class);
    private final FeedController controller = new FeedController(mapper);

    @Test
    void summaryPrefersExplicitSummary() {
        Article a = new Article();
        a.setSummary("hand written");
        a.setContentMd("# body");
        assertThat(controller.summaryOf(a)).isEqualTo("hand written");
    }

    @Test
    void summaryFallsBackToPlainTextOfBody() {
        Article a = new Article();
        a.setContentMd("# 标题\n\n这是 **正文**，带 [链接](https://x.com)。");

        String summary = controller.summaryOf(a);

        assertThat(summary).doesNotContain("#").doesNotContain("**").doesNotContain("](");
        assertThat(summary).contains("标题").contains("正文");
    }

    @Test
    void summaryIsTruncated() {
        Article a = new Article();
        a.setContentMd("字".repeat(500));
        assertThat(controller.summaryOf(a)).hasSize(FeedController.SUMMARY_LENGTH + 1).endsWith("…");
    }

    @Test
    void rssRendersValidDatesAndEncodedLinks() throws Exception {
        Article a = new Article();
        a.setTitle("A & B");
        a.setSlug("中文 slug");
        a.setSummary("s");
        a.setCreateTime(LocalDateTime.of(2026, 4, 30, 10, 43, 18));
        when(mapper.selectPublished()).thenReturn(List.of(a));

        MockHttpServletResponse resp = new MockHttpServletResponse();
        controller.rss(resp);
        String xml = resp.getContentAsString();

        assertThat(xml).contains("<title>A &amp; B</title>");
        assertThat(xml).contains("https://www.xiaolai.cyou/articles/%E4%B8%AD%E6%96%87%20slug");
        assertThat(xml).containsPattern("<pubDate>Thu, 30 Apr 2026 10:43:18 [+-]\\d{4}</pubDate>");
    }
}
