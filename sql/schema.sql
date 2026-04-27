-- Xiaolai Blog Database Schema

CREATE DATABASE IF NOT EXISTS xiaolai_blog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE xiaolai_blog;

-- User table (single admin)
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL COMMENT 'Username',
    `password` VARCHAR(255) NOT NULL COMMENT 'Bcrypt encrypted password',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT 'Display name',
    `email` VARCHAR(100) DEFAULT NULL COMMENT 'Email',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'Avatar URL',
    `bio` VARCHAR(500) DEFAULT NULL COMMENT 'Biography',
    `login_attempts` INT DEFAULT 0 COMMENT 'Consecutive login failures',
    `lock_time` DATETIME DEFAULT NULL COMMENT 'Account lock time',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User (admin)';

-- Category table
CREATE TABLE `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT 'Category name',
    `slug` VARCHAR(100) NOT NULL COMMENT 'URL-friendly identifier',
    `description` VARCHAR(200) DEFAULT NULL COMMENT 'Optional description',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_slug` (`slug`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Article categories';

-- Tag table
CREATE TABLE `tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT 'Tag name',
    `slug` VARCHAR(100) NOT NULL COMMENT 'URL-friendly identifier',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_slug` (`slug`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Article tags';

-- Article table
CREATE TABLE `article` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL COMMENT 'Article title',
    `slug` VARCHAR(200) NOT NULL COMMENT 'URL-friendly identifier',
    `summary` VARCHAR(500) DEFAULT NULL COMMENT 'Article summary / excerpt',
    `content_md` MEDIUMTEXT NOT NULL COMMENT 'Markdown source',
    `content_html` MEDIUMTEXT NOT NULL COMMENT 'Rendered HTML',
    `cover_url` VARCHAR(500) DEFAULT NULL COMMENT 'Cover image URL',
    `category_id` BIGINT DEFAULT NULL COMMENT 'Category ID',
    `status` TINYINT DEFAULT 0 COMMENT '0=Draft, 1=Published',
    `is_top` TINYINT DEFAULT 0 COMMENT '0=Normal, 1=Pinned',
    `views` INT DEFAULT 0 COMMENT 'Page views',
    `password` VARCHAR(255) DEFAULT NULL COMMENT 'Optional password for protected articles',
    `seo_title` VARCHAR(200) DEFAULT NULL COMMENT 'Custom SEO title',
    `seo_description` VARCHAR(500) DEFAULT NULL COMMENT 'Custom SEO description',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0 COMMENT 'Soft delete flag',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_slug` (`slug`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_is_top` (`is_top`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Articles';

-- Article-Tag relationship
CREATE TABLE `article_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `article_id` BIGINT NOT NULL,
    `tag_id` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_tag` (`article_id`, `tag_id`),
    KEY `idx_tag_id` (`tag_id`),
    CONSTRAINT `fk_at_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_at_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Article-Tag mapping';

-- Comment table
CREATE TABLE `comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `article_id` BIGINT NOT NULL COMMENT 'Article ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT 'Parent comment ID for nested replies',
    `nickname` VARCHAR(100) NOT NULL COMMENT 'Commenter name',
    `email` VARCHAR(100) NOT NULL COMMENT 'Commenter email',
    `website` VARCHAR(200) DEFAULT NULL COMMENT 'Optional website URL',
    `content_md` TEXT NOT NULL COMMENT 'Markdown content',
    `content_html` TEXT NOT NULL COMMENT 'Rendered HTML',
    `status` TINYINT DEFAULT 0 COMMENT '0=Pending, 1=Approved, 2=Rejected, 3=Spam',
    `is_admin` TINYINT DEFAULT 0 COMMENT '1=Admin reply',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_comment_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Comments';

-- Media / file table
CREATE TABLE `media` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `original_name` VARCHAR(255) NOT NULL COMMENT 'Original filename',
    `stored_name` VARCHAR(255) NOT NULL COMMENT 'Stored filename',
    `url` VARCHAR(500) NOT NULL COMMENT 'Access URL',
    `path` VARCHAR(500) NOT NULL COMMENT 'Server storage path',
    `type` VARCHAR(50) DEFAULT NULL COMMENT 'MIME type',
    `size` BIGINT DEFAULT 0 COMMENT 'File size in bytes',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Media files';

-- Site config table (key-value)
CREATE TABLE `site_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `config_key` VARCHAR(100) NOT NULL COMMENT 'Configuration key',
    `config_value` TEXT COMMENT 'Configuration value',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Site configuration';

-- Login log table
CREATE TABLE `login_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `ip` VARCHAR(50) DEFAULT NULL,
    `success` TINYINT DEFAULT 0 COMMENT '0=Failed, 1=Success',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_username` (`username`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Login logs';

-- Insert default admin user (password: admin123, please change after first login)
INSERT INTO `user` (`username`, `password`, `nickname`, `email`) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Admin', 'admin@xiaolai.com');

-- Insert default site config
INSERT INTO `site_config` (`config_key`, `config_value`) VALUES
('site_title', 'Xiaolai Blog'),
('site_subtitle', 'Share knowledge, inspire thinking'),
('site_description', 'A personal blog about technology and life'),
('site_logo', ''),
('site_favicon', ''),
('copyright', 'Copyright © 2024 Xiaolai Blog. All rights reserved.'),
('social_github', ''),
('social_email', ''),
('seo_keywords', 'blog, technology, personal'),
('seo_description', 'Xiaolai personal blog'),
('about_content', ''),
('about_content_md', '');