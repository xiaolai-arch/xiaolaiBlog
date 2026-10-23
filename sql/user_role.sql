-- 用户角色：区分管理员与普通注册用户（普通用户可用学习工具，不能进管理后台）
ALTER TABLE `user` ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT 'admin/user';
UPDATE `user` SET `role` = 'admin' WHERE `username` = 'admin';
