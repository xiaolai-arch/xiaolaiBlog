package com.xiaolai.blog.controller.front;

import com.xiaolai.blog.entity.Article;
import com.xiaolai.blog.mapper.ArticleMapper;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class FeedController {

    private static final String SITE_URL = "https://www.xiaolai.cyou";
    static final int SUMMARY_LENGTH = 200;

    private final ArticleMapper articleMapper;
    private final Parser parser = Parser.builder().build();
    private final TextContentRenderer textRenderer = TextContentRenderer.builder().build();

    public FeedController(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @GetMapping("/rss")
    public void rss(HttpServletResponse response) throws IOException {
        List<Article> articles = articleMapper.selectPublished();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss version=\"2.0\">\n<channel>\n");
        xml.append("<title>Xiaolai Blog</title>\n");
        xml.append("<link>").append(SITE_URL).append("</link>\n");
        xml.append("<description>Xiaolai Blog RSS Feed</description>\n");

        for (Article article : articles) {
            String link = SITE_URL + "/articles/" + URLEncoder.encode(article.getSlug(), StandardCharsets.UTF_8).replace("+", "%20");
            xml.append("<item>\n");
            xml.append("<title>").append(escapeXml(article.getTitle())).append("</title>\n");
            xml.append("<link>").append(escapeXml(link)).append("</link>\n");
            xml.append("<guid isPermaLink=\"true\">").append(escapeXml(link)).append("</guid>\n");
            xml.append("<description>").append(escapeXml(summaryOf(article))).append("</description>\n");
            xml.append("<pubDate>").append(article.getCreateTime().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.RFC_1123_DATE_TIME)).append("</pubDate>\n");
            xml.append("</item>\n");
        }

        xml.append("</channel>\n</rss>");

        response.setContentType("application/rss+xml; charset=UTF-8");
        response.getWriter().write(xml.toString());
    }

    /** Uses the hand-written summary when present, otherwise the first characters of the body as plain text. */
    String summaryOf(Article article) {
        if (article.getSummary() != null && !article.getSummary().isBlank()) {
            return article.getSummary();
        }
        if (article.getContentMd() == null) return "";
        String text = textRenderer.render(parser.parse(article.getContentMd()))
                .replaceAll("\\s+", " ")
                .trim();
        return text.length() > SUMMARY_LENGTH ? text.substring(0, SUMMARY_LENGTH) + "…" : text;
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
