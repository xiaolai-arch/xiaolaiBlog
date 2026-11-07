-- Feature upgrade; compatible with MySQL 5.7+, repeatable without changing existing data.
SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE article ADD COLUMN publish_at DATETIME NULL', 'SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='article' AND column_name='publish_at');
PREPARE feature_stmt FROM @ddl; EXECUTE feature_stmt; DEALLOCATE PREPARE feature_stmt;
SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE article ADD COLUMN series_name VARCHAR(100) NOT NULL DEFAULT ''''', 'SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='article' AND column_name='series_name');
PREPARE feature_stmt FROM @ddl; EXECUTE feature_stmt; DEALLOCATE PREPARE feature_stmt;
SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE article ADD COLUMN series_order INT NOT NULL DEFAULT 0', 'SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='article' AND column_name='series_order');
PREPARE feature_stmt FROM @ddl; EXECUTE feature_stmt; DEALLOCATE PREPARE feature_stmt;
SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE note ADD COLUMN folder VARCHAR(100) NOT NULL DEFAULT ''''', 'SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='note' AND column_name='folder');
PREPARE feature_stmt FROM @ddl; EXECUTE feature_stmt; DEALLOCATE PREPARE feature_stmt;

CREATE TABLE IF NOT EXISTS content_revision (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  kind VARCHAR(16) NOT NULL,
  resource_id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  content MEDIUMTEXT NOT NULL,
  metadata MEDIUMTEXT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_revision_resource (kind, resource_id, owner_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS account_email (
  user_id BIGINT PRIMARY KEY,
  email VARCHAR(254) NOT NULL,
  verified TINYINT NOT NULL DEFAULT 0,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS account_token (
  token_hash CHAR(64) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  purpose VARCHAR(16) NOT NULL,
  email VARCHAR(254) NOT NULL,
  expires_at DATETIME NOT NULL,
  KEY idx_account_token_user (user_id, purpose)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS study_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  source_url VARCHAR(500) NOT NULL DEFAULT '',
  state VARCHAR(16) NOT NULL DEFAULT 'todo',
  note_id BIGINT NULL,
  review_at DATETIME NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_study_owner (user_id, review_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
