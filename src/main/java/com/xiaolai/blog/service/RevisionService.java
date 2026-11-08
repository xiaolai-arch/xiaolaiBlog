package com.xiaolai.blog.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import com.xiaolai.blog.common.exception.BlogException;

/** Revisions hold snapshots before an edit; access is enforced by the resource controller. */
@Service
public class RevisionService {
    private final JdbcTemplate db;
    public RevisionService(JdbcTemplate db) { this.db = db; }

    public void save(String kind, long id, long owner, String title, String content, String metadata) {
        db.update("INSERT INTO content_revision(kind,resource_id,owner_id,title,content,metadata) VALUES(?,?,?,?,?,?)",
                kind, id, owner, title == null ? "" : title, content == null ? "" : content, metadata);
        // Retain the most recent 50 snapshots for each resource.
        List<Long> ids = db.queryForList("SELECT id FROM content_revision WHERE kind=? AND resource_id=? AND owner_id=? ORDER BY id DESC", Long.class, kind, id, owner);
        for (int i = 50; i < ids.size(); i++) db.update("DELETE FROM content_revision WHERE id=?", ids.get(i));
    }

    public void purge(String kind, long id, long owner) {
        db.update("DELETE FROM content_revision WHERE kind=? AND resource_id=? AND owner_id=?", kind, id, owner);
        if (kind.equals("note")) db.update("UPDATE study_item SET note_id=NULL WHERE note_id=? AND user_id=?", id, owner);
    }

    public List<Map<String, Object>> list(String kind, long id, long owner) {
        return db.queryForList("SELECT id,title,create_time AS createTime FROM content_revision WHERE kind=? AND resource_id=? AND owner_id=? ORDER BY id DESC", kind, id, owner);
    }

    public Map<String, Object> get(String kind, long id, long owner, long revision) {
        var rows = db.queryForList("SELECT id,title,content,metadata,create_time AS createTime FROM content_revision WHERE kind=? AND resource_id=? AND owner_id=? AND id=?", kind, id, owner, revision);
        if (rows.isEmpty()) throw new BlogException(404, "历史版本不存在");
        return rows.getFirst();
    }

    public long noteStorageBytes(long owner) {
        Long bytes = db.queryForObject(
                "SELECT COALESCE(SUM(COALESCE(OCTET_LENGTH(content),0) + COALESCE(OCTET_LENGTH(metadata),0)),0) " +
                        "FROM content_revision WHERE kind='note' AND owner_id=?",
                Long.class, owner);
        return bytes == null ? 0 : bytes;
    }
}
