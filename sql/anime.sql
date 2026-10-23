-- 番剧收藏：首页动漫小组件数据源（后台维护，封面可选、缺省渐变占位）
CREATE TABLE IF NOT EXISTS `anime` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(100) NOT NULL COMMENT '番剧名',
  `cover_url` VARCHAR(255) DEFAULT NULL COMMENT '封面地址（可空，前端渐变占位）',
  `status` VARCHAR(20) DEFAULT 'watched' COMMENT 'watched=看完 / watching=在看 / wish=想看',
  `rating` TINYINT DEFAULT NULL COMMENT '评分 1-10，可空',
  `comment` VARCHAR(200) DEFAULT '' COMMENT '一句话感想',
  `sort_order` INT DEFAULT 0 COMMENT '展示顺序，小的在前',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='番剧收藏';
