-- Gallery feature migration
-- Run this against xiaolai_blog database

-- 1. Create media_category table
CREATE TABLE IF NOT EXISTS `media_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT 'Category name',
    `slug` VARCHAR(100) NOT NULL COMMENT 'URL-friendly identifier',
    `description` VARCHAR(200) DEFAULT NULL COMMENT 'Optional description',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mc_slug` (`slug`),
    UNIQUE KEY `uk_mc_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Media gallery categories';

-- 2. Add title and category_id to media table
ALTER TABLE `media`
    ADD COLUMN `title` VARCHAR(200) DEFAULT NULL COMMENT 'Image caption / title' AFTER `original_name`,
    ADD COLUMN `category_id` BIGINT DEFAULT NULL COMMENT 'Media category ID' AFTER `title`;

-- 3. Insert a default category
INSERT INTO `media_category` (`name`, `slug`, `description`) VALUES
('未分类', 'uncategorized', 'Default category');