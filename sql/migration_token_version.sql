-- 幂等迁移：发布认证版本校验前执行。旧令牌会失效，用户需重新登录。
SET @add_token_version = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `user` ADD COLUMN `token_version` BIGINT NOT NULL DEFAULT 0',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'token_version'
);
PREPARE migration_stmt FROM @add_token_version;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;
