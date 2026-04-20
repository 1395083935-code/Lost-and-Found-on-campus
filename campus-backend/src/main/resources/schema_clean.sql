-- 校园失物招领系统 - 数据库初始化脚本
-- 创建时间: 2026-04-17

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `campus_db` DEFAULT CHARSET=utf8mb4;
USE `campus_db`;

-- 禁用外键检查以便删除表
SET FOREIGN_KEY_CHECKS=0;

-- 删除旧表（如果存在）
DROP TABLE IF EXISTS `favorite`;
DROP TABLE IF EXISTS `notice`;
DROP TABLE IF EXISTS `report`;
DROP TABLE IF EXISTS `lost_found_item`;
DROP TABLE IF EXISTS `lost_and_found_item`;
DROP TABLE IF EXISTS `admin`;
DROP TABLE IF EXISTS `user`;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS=1;

CREATE TABLE `user` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（bcrypt加密）',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `contact_info` VARCHAR(255) DEFAULT NULL COMMENT '联系方式（手机号/微信）',
  `student_id` VARCHAR(20) DEFAULT NULL COMMENT '学号/工号',
  `role` TINYINT DEFAULT 0 COMMENT '角色: 0普通用户, 1管理员',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1正常, 0封禁',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 管理员表
CREATE TABLE `admin` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '管理员账号',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（bcrypt加密）',
  `role` TINYINT DEFAULT 2 COMMENT '角色: 1超级管理员, 2普通管理员',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1正常, 0禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_admin_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 3. 失物招领信息表
CREATE TABLE `lost_found_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(100) NOT NULL COMMENT '物品名称（2-20字）',
  `description` TEXT COMMENT '物品特征描述（0-500字）',
  `type` TINYINT NOT NULL COMMENT '类型: 0失物, 1招领',
  `category` VARCHAR(64) NOT NULL COMMENT '分类: 证件/电子/文具/衣物/其他',
  `location` VARCHAR(100) NOT NULL COMMENT '丢失/拾获地点',
  `storage_location` VARCHAR(100) DEFAULT NULL COMMENT '存放地点（仅拾物）',
  `is_anonymous` TINYINT DEFAULT 0 COMMENT '是否匿名: 1是, 0否',
  `image_url` VARCHAR(500) DEFAULT NULL COMMENT '图片URL（多个用逗号分隔）',
  `contact_info` VARCHAR(20) NOT NULL COMMENT '联系方式',
  `user_id` INT NOT NULL COMMENT '发布者用户ID',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0待审核, 1已通过, 2已驳回, 3已完结',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_type` (`type`),
  KEY `idx_category` (`category`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_item_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物招领信息表';

-- 4. 收藏表
CREATE TABLE `favorite` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `item_id` BIGINT NOT NULL COMMENT '物品ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_user_item` (`user_id`, `item_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_item_id` (`item_id`),
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_favorite_item` FOREIGN KEY (`item_id`) REFERENCES `lost_found_item`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- 5. 消息通知表
CREATE TABLE `notice` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` INT NOT NULL COMMENT '接收通知的用户ID',
  `item_id` BIGINT DEFAULT NULL COMMENT '相关物品ID',
  `content` VARCHAR(255) NOT NULL COMMENT '通知内容',
  `type` TINYINT NOT NULL COMMENT '通知类型: 1审核通过, 2审核驳回, 3匹配提醒, 4举报处理',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读: 1已读, 0未读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_notice_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

-- 6. 举报表
CREATE TABLE `report` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `item_id` BIGINT NOT NULL COMMENT '被举报的物品ID',
  `reporter_id` INT NOT NULL COMMENT '举报人用户ID',
  `reason` VARCHAR(255) NOT NULL COMMENT '举报原因: 虚假信息/垃圾广告/恶意骚扰',
  `description` TEXT DEFAULT NULL COMMENT '详细说明',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0待处理, 1已处理, 2已驳回',
  `handler_id` INT DEFAULT NULL COMMENT '处理人管理员ID',
  `handle_reason` VARCHAR(255) DEFAULT NULL COMMENT '处理说明',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_item_id` (`item_id`),
  KEY `idx_reporter_id` (`reporter_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_report_item` FOREIGN KEY (`item_id`) REFERENCES `lost_found_item`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报表';

-- 数据初始化示例：创建一个管理员账号 (密码: admin123)
-- 密码使用 BCrypt 加密: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E36tF48m2
INSERT IGNORE INTO `admin` (`username`, `password`, `role`, `status`) 
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E36tF48m2', 1, 1);
