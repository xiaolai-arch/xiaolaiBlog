package com.xiaolai.blog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaolai.blog.service.ArticleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=${TEST_DATABASE_URL:jdbc:h2:mem:regression;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER,VALUE}",
        "spring.datasource.driver-class-name=${TEST_DATABASE_DRIVER:org.h2.Driver}",
        "spring.datasource.username=${TEST_DATABASE_USERNAME:sa}", "spring.datasource.password=${TEST_DATABASE_PASSWORD:}",
        "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"
})
@AutoConfigureMockMvc
class RegressionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired PasswordEncoder passwords;
    @Autowired ObjectMapper json;
    @Autowired ArticleService articles;

    @BeforeEach
    void schema() throws Exception {
        try (var connection = db.getDataSource().getConnection()) {
            if ("MySQL".equals(connection.getMetaData().getDatabaseProductName())) {
                // Only the disposable databases created by tools/verify_mysql.sh may be cleared.
                assertTrue(connection.getCatalog().matches("xiaolai_test_[a-z0-9_]+"), "A disposable test database is required");
                for (String table : new String[]{"article_tag", "comment", "article", "category", "tag", "note", "user_progress", "user"}) {
                    db.execute("DELETE FROM `" + table + "`");
                }
                createAdmin();
                return;
            }
        }
        db.execute("DROP ALL OBJECTS");
        db.execute("CREATE TABLE user (id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(30) UNIQUE, password VARCHAR(255), " +
                "nickname VARCHAR(50), email VARCHAR(100), avatar VARCHAR(255), bio VARCHAR(255), role VARCHAR(20) DEFAULT 'user', " +
                "token_version BIGINT DEFAULT 0 NOT NULL, login_attempts INT DEFAULT 0, lock_time TIMESTAMP, create_time TIMESTAMP, update_time TIMESTAMP)");
        db.execute("CREATE TABLE article(id BIGINT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(100), slug VARCHAR(100), summary VARCHAR(100), " +
                "content_md CLOB, content_html CLOB, cover_url VARCHAR(255), category_id BIGINT, status INT, is_top INT, views INT, " +
                "password VARCHAR(255), seo_title VARCHAR(100), seo_description VARCHAR(100), create_time TIMESTAMP, update_time TIMESTAMP, deleted INT DEFAULT 0)");
        db.execute("CREATE TABLE category(id BIGINT PRIMARY KEY, name VARCHAR(100), slug VARCHAR(100))");
        db.execute("CREATE TABLE tag(id BIGINT PRIMARY KEY, name VARCHAR(100), slug VARCHAR(100))");
        db.execute("CREATE TABLE article_tag(id BIGINT PRIMARY KEY, article_id BIGINT, tag_id BIGINT)");
        db.execute("CREATE TABLE note(id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, title VARCHAR(200), content CLOB, create_time TIMESTAMP, update_time TIMESTAMP, deleted INT DEFAULT 0)");
        db.execute("CREATE TABLE user_progress(id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, prog_key VARCHAR(64), value CLOB, create_time TIMESTAMP, update_time TIMESTAMP, UNIQUE(user_id, prog_key))");
        createAdmin();
    }

    private void createAdmin() {
        db.update("INSERT INTO user(username,password,nickname,role) VALUES(?,?,?,?)", "admin", passwords.encode("original123"), "Admin", "admin");
    }

    private String login(String password) throws Exception {
        var response = mvc.perform(post("/api/v1/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("username", "admin", "password", password))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).path("data").path("token").asText();
    }

    @Test
    void changingPasswordAndLogoutRevokePreviousTokens() throws Exception {
        String old = login("original123");
        mvc.perform(put("/api/v1/admin/profile/password").header("Authorization", "Bearer " + old)
                .contentType(MediaType.APPLICATION_JSON).content("{\"oldPassword\":\"original123\",\"newPassword\":\"replacement123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/v1/admin/articles").header("Authorization", "Bearer " + old)).andExpect(status().isUnauthorized());
        String fresh = login("replacement123");
        mvc.perform(get("/api/v1/admin/articles").header("Authorization", "Bearer " + fresh)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/auth/logout").header("Authorization", "Bearer " + fresh)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/articles").header("Authorization", "Bearer " + fresh)).andExpect(status().isUnauthorized());
    }

    @Test
    void currentDatabaseRoleOverridesRoleInToken() throws Exception {
        String token = login("original123");
        db.update("UPDATE user SET role='user' WHERE username='admin'");
        mvc.perform(get("/api/v1/admin/articles").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test
    void invalidRegistrationIsRejectedBeforeAccessingDatabase() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"valid1234\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"ab\",\"password\":\"valid1234\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM user", Integer.class));
    }

    @Test
    void deletedArticleCanBeRestoredAndMissingRestoreIsReported() {
        db.update("INSERT INTO article(id,title,slug,content_md,content_html,deleted) VALUES(1,'Deleted','deleted','','',1)");
        articles.restore(1L);
        assertEquals(0, db.queryForObject("SELECT deleted FROM article WHERE id=1", Integer.class));
        assertThrows(com.xiaolai.blog.common.exception.BlogException.class, () -> articles.restore(1L));
    }

    @Test
    void publicPaginationFiltersInDatabaseAndOmitsBody() throws Exception {
        db.update("INSERT INTO category(id,name,slug) VALUES(1,'Java','java')");
        db.update("INSERT INTO tag(id,name,slug) VALUES(1,'Spring','spring')");
        for (int i = 1; i <= 3; i++) {
            db.update("INSERT INTO article(id,title,slug,content_md,content_html,category_id,status,is_top,deleted,create_time) VALUES(?,?,?,'long body','long body',1,1,0,0,'2026-01-01 12:00:00')", i, "Match " + i, "post-" + i);
            db.update("INSERT INTO article_tag VALUES(?, ?, 1)", i, i);
        }
        mvc.perform(get("/api/v1/articles").param("page", "2").param("size", "1").param("category", "java").param("tag", "spring").param("keyword", "Match"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.list.length()").value(1)).andExpect(jsonPath("$.data.list[0].slug").value("post-2"))
                .andExpect(jsonPath("$.data.list[0].contentMd").isEmpty()).andExpect(jsonPath("$.data.list[0].tags[0].slug").value("spring"));
        mvc.perform(get("/api/v1/articles").param("page", "-1")).andExpect(jsonPath("$.code").value(422));
        mvc.perform(get("/api/v1/articles").param("size", "10000")).andExpect(jsonPath("$.code").value(422));
        mvc.perform(get("/api/v1/articles/post-2/navigation"))
                .andExpect(jsonPath("$.data.previous.slug").value("post-1"))
                .andExpect(jsonPath("$.data.next.slug").value("post-3"));
    }

    @Test
    void registrationCreatesIsolatedUserAndRejectsDuplicate() throws Exception {
        String payload = "{\"username\":\"new-user\",\"password\":\"valid1234\"}";
        var result = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.user.role").value("user"))
                .andExpect(jsonPath("$.data.user.id").isNumber()).andReturn();
        String token = json.readTree(result.getResponse().getContentAsString()).path("data").path("token").asText();
        mvc.perform(get("/api/v1/admin/articles").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(jsonPath("$.code").value(422));
    }

    @Test
    void noteSummaryOmitsContentAndOtherUsersCannotReadNote() throws Exception {
        Long userId = db.queryForObject("SELECT id FROM user WHERE username='admin'", Long.class);
        db.update("INSERT INTO note(id,user_id,title,content) VALUES(1,?,'Private','secret body')", userId);
        db.update("INSERT INTO note(id,user_id,title,content) VALUES(2,999,'Someone else','secret')");
        String token = login("original123");
        mvc.perform(get("/api/v1/notes").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].content").doesNotExist());
        mvc.perform(get("/api/v1/notes/1").header("Authorization", "Bearer " + token)).andExpect(jsonPath("$.data.content").value("secret body"));
        mvc.perform(get("/api/v1/notes/2").header("Authorization", "Bearer " + token)).andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void progressMergesIndependentUpdatesAndPreservesCancellation() throws Exception {
        String token = login("original123");
        String a = "{\"version\":2,\"items\":{\"a\":{\"done\":true,\"at\":10}}}";
        String b = "{\"version\":2,\"items\":{\"b\":{\"done\":true,\"at\":10},\"a\":{\"done\":false,\"at\":20}}}";
        for (String value : new String[]{a, b, a}) {
            mvc.perform(put("/api/v1/progress/leetcode").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("value", value))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        }
        var response = mvc.perform(get("/api/v1/progress/leetcode").header("Authorization", "Bearer " + token)).andReturn();
        var state = json.readTree(json.readTree(response.getResponse().getContentAsString()).path("data").path("value").asText());
        assertFalse(state.path("items").path("a").path("done").asBoolean());
        assertTrue(state.path("items").path("b").path("done").asBoolean());
    }

    @Test
    void simultaneousProgressUploadsRetainBothDevicesUpdates() throws Exception {
        String token = login("original123");
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var workers = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var uploads = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (String item : new String[]{"device-a", "device-b"}) {
                uploads.add(workers.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(10, java.util.concurrent.TimeUnit.SECONDS));
                    String value = json.writeValueAsString(java.util.Map.of("version", 2, "items",
                            java.util.Map.of(item, java.util.Map.of("done", true, "at", 100))));
                    mvc.perform(put("/api/v1/progress/leetcode").header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("value", value))))
                            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
                    return null;
                }));
            }
            assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            for (var upload : uploads) upload.get(15, java.util.concurrent.TimeUnit.SECONDS);
        }
        String value = db.queryForObject("SELECT `value` FROM user_progress WHERE prog_key='leetcode'", String.class);
        var items = json.readTree(value).path("items");
        assertTrue(items.path("device-a").path("done").asBoolean());
        assertTrue(items.path("device-b").path("done").asBoolean());
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM user_progress", Integer.class));
    }
}
