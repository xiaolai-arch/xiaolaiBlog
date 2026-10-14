-- 云端笔记表（幂等，可重复执行）
CREATE TABLE IF NOT EXISTS `note` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '笔记ID',
  `user_id`     BIGINT       NOT NULL COMMENT '所属用户ID',
  `title`       VARCHAR(200) NOT NULL DEFAULT '' COMMENT '标题',
  `content`     MEDIUMTEXT   NULL COMMENT 'Markdown 内容',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_update` (`user_id`, `update_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '云端笔记';
