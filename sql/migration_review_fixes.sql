-- Idempotent data repair and integrity migration for confirmed review findings.

-- Give historical article cards a short summary without shipping the full body.
UPDATE article
SET summary = LEFT(TRIM(content_md), 500)
WHERE summary IS NULL OR TRIM(summary) = '';

-- Repair unusable empty slugs while retaining stable, unique values.
UPDATE category
SET slug = CONCAT('category-', id)
WHERE slug IS NULL OR TRIM(slug) = '';

UPDATE tag
SET slug = CONCAT('tag-', id)
WHERE slug IS NULL OR TRIM(slug) = '';

UPDATE article
SET slug = CONCAT('article-', id)
WHERE slug IS NULL OR TRIM(slug) = '';

-- Remove forged cross-article/nested replies and replies whose parent no longer exists.
DELETE child
FROM comment child
JOIN comment parent ON child.parent_id = parent.id
WHERE child.article_id <> parent.article_id OR parent.parent_id IS NOT NULL;

DELETE child
FROM comment child
LEFT JOIN comment parent ON child.parent_id = parent.id
WHERE child.parent_id IS NOT NULL AND parent.id IS NULL;

-- Enforce parent cleanup for databases created before the self-referencing FK.
SET @has_comment_parent_fk = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'comment'
      AND constraint_name = 'fk_comment_parent'
      AND constraint_type = 'FOREIGN KEY'
);
SET @comment_parent_ddl = IF(
    @has_comment_parent_fk = 0,
    'ALTER TABLE comment ADD CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comment (id) ON DELETE CASCADE',
    'SELECT 1'
);
PREPARE comment_parent_stmt FROM @comment_parent_ddl;
EXECUTE comment_parent_stmt;
DEALLOCATE PREPARE comment_parent_stmt;

-- Preserve row counts while disabling the old unusable placeholder.
UPDATE `user`
SET password = '!disabled-no-login-placeholder'
WHERE username = 'admin'
  AND password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';
