package com.xiaolai.blog.controller.front;

import com.xiaolai.blog.entity.Article;
import com.xiaolai.blog.mapper.ArticleMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class FeedController {

    private final ArticleMapper articleMapper;

    public FeedController(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @GetMapping("/rss")
    public void rss(HttpServletResponse response) throws IOException {
        List<Article> articles = articleMapper.selectPublished();
        if (articles.size() > 20) articles = articles.subList(0, 20);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss version=\"2.0\">\n<channel>\n");
        xml.append("<title>Xiaolai Blog</title>\n");
        xml.append("<link>https://xiaolai.com</link>\n");
        xml.append("<description>Xiaolai Blog RSS Feed</description>\n");

        for (Article article : articles) {
            xml.append("<item>\n");
            xml.append("<title>").append(escapeXml(article.getTitle())).append("</title>\n");
            xml.append("<link>https://xiaolai.com/articles/").append(article.getSlug()).append("</link>\n");
            xml.append("<description>").append(escapeXml(article.getSummary())).append("</description>\n");
            xml.append("<pubDate>").append(article.getCreateTime().format(DateTimeFormatter.RFC_1123_DATE_TIME)).append("</pubDate>\n");
            xml.append("</item>\n");
        }

        xml.append("</channel>\n</rss>");

        response.setContentType("application/rss+xml; charset=UTF-8");
        response.getWriter().write(xml.toString());
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}