-- 用户学习进度表（幂等，可重复执行）：键值型，value 为 JSON 字符串
CREATE TABLE IF NOT EXISTS `user_progress` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id`    BIGINT      NOT NULL COMMENT '所属用户ID',
  `prog_key`   VARCHAR(64) NOT NULL COMMENT '进度键（如 leetcode / cet4）',
  `value`      MEDIUMTEXT  NULL COMMENT '进度内容（JSON）',
  `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_key` (`user_id`, `prog_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户学习进度';
