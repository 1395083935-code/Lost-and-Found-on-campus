-- ============================================
-- 校园失物招领 - 数据库初始化脚本
-- 适配 MyBatis Plus + Spring Boot 3.x
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `campus_db` DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
USE `campus_db`;

-- ============================================
-- 1. 用户表 (user)
-- ============================================
CREATE TABLE IF NOT EXISTS `user` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码(bcrypt加密)',
  `avatar` VARCHAR(255) COMMENT '头像URL',
  `contact_info` VARCHAR(255) COMMENT '联系方式(电话)',
  `student_id` VARCHAR(20) COMMENT '学号/工号',
  `role` TINYINT DEFAULT 0 COMMENT '角色: 0-普通用户 1-管理员',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1-正常 0-封禁',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 2. 管理员表 (admin)
-- ============================================
CREATE TABLE IF NOT EXISTS `admin` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '管理员账号',
  `password` VARCHAR(100) NOT NULL COMMENT '密码(bcrypt加密)',
  `role` TINYINT DEFAULT 2 COMMENT '角色: 1-超级管理员 2-普通管理员',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1-正常 0-禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- ============================================
-- 3. 失物招领信息表 (lost_found_item)
-- ============================================
CREATE TABLE IF NOT EXISTS `lost_found_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '物品ID',
  `title` VARCHAR(255) NOT NULL COMMENT '物品名称(2-20字)',
  `description` TEXT COMMENT '物品描述(0-500字)',
  `type` TINYINT NOT NULL COMMENT '类型: 0-失物 1-招领',
  `category` VARCHAR(64) COMMENT '分类: 证件/电子/文具/衣物/其他',
  `location` VARCHAR(255) COMMENT '丢失/拾获地点',
  `storage_location` VARCHAR(100) COMMENT '存放地点(仅拾物)',
  `is_anonymous` TINYINT DEFAULT 0 COMMENT '匿名发布: 1-是 0-否',
  `image_url` VARCHAR(500) COMMENT '图片URL(多个逗号分隔)',
  `contact_info` VARCHAR(255) NOT NULL COMMENT '联系方式(电话)',
  `user_id` INT COMMENT '发布者用户ID',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待审核 1-已通过 2-已驳回 3-已完结',
  `reject_reason` VARCHAR(255) COMMENT '驳回原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_type` (`type`),
  KEY `idx_category` (`category`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_item_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物招领信息表';

-- ============================================
-- 4. 用户收藏表 (favorite)
-- ============================================
CREATE TABLE IF NOT EXISTS `favorite` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `item_id` BIGINT NOT NULL COMMENT '物品ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_item` (`user_id`, `item_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_item_id` (`item_id`),
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_favorite_item` FOREIGN KEY (`item_id`) REFERENCES `lost_found_item`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- ============================================
-- 5. 通知表 (notice)
-- ============================================
CREATE TABLE IF NOT EXISTS `notice` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` INT NOT NULL COMMENT '接收通知的用户ID',
  `item_id` BIGINT COMMENT '相关物品ID',
  `content` TEXT NOT NULL COMMENT '通知内容',
  `type` TINYINT COMMENT '类型: 1-审核通过 2-审核驳回 3-匹配提醒 4-举报处理',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读: 1-已读 0-未读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_notice_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- ============================================
-- 6. 举报表 (report)
-- ============================================
CREATE TABLE IF NOT EXISTS `report` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '举报ID',
  `item_id` BIGINT NOT NULL COMMENT '被举报的物品ID',
  `reporter_id` INT NOT NULL COMMENT '举报人用户ID',
  `reason` VARCHAR(100) COMMENT '举报原因: 虚假信息/垃圾广告/恶意骚扰',
  `description` TEXT COMMENT '详细说明',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待处理 1-已处理 2-已驳回',
  `handler_id` INT COMMENT '处理人管理员ID',
  `handle_reason` VARCHAR(255) COMMENT '处理说明',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_item_id` (`item_id`),
  KEY `idx_reporter_id` (`reporter_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_report_item` FOREIGN KEY (`item_id`) REFERENCES `lost_found_item`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_report_handler` FOREIGN KEY (`handler_id`) REFERENCES `admin`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报表';

-- ============================================
-- 7. 搜索热词统计表 (search_hotwords) - P2-4搜索优化
-- ============================================
CREATE TABLE IF NOT EXISTS `search_hotwords` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `keyword` VARCHAR(255) NOT NULL COMMENT '搜索关键词',
  `search_count` INT DEFAULT 0 COMMENT '搜索次数',
  `last_search_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后搜索时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_keyword` (`keyword`),
  KEY `idx_search_count` (`search_count` DESC),
  KEY `idx_last_search_time` (`last_search_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索关键词热度统计表';

-- ============================================
-- 初始数据插入
-- ============================================

-- 创建默认管理员账号(密码: admin123)
INSERT IGNORE INTO `admin` (`username`, `password`, `role`, `status`) VALUES
('admin', '$2a$10$slYQmyNdGzin7olVN3p5Be7DlH.PKZbv5H8KnzzVgXXbVxzy990RK', 1, 1);

-- 创建测试用户(密码: user123)
INSERT IGNORE INTO `user` (`username`, `password`, `role`, `status`, `student_id`, `contact_info`) VALUES
('testuser', '$2a$10$YY4k7z1j8l5kx9m4n3o2p1Gq6r7s8t9uV0w1x2y3z4a5b6c7d8', 0, 1, '20210001', '13800138000'),
('admin', '$2a$10$slYQmyNdGzin7olVN3p5Be7DlH.PKZbv5H8KnzzVgXXbVxzy990RK', 1, 1, '20210002', '13800138001');

-- 创建测试失物招领信息
INSERT IGNORE INTO `lost_found_item` (`id`, `title`, `type`, `category`, `location`, `contact_info`, `user_id`, `status`, `description`) VALUES
(1, 'iPhone 13 Pro', 0, '电子', '图书馆', '13800138000', 1, 1, '深空黑色，已放在失物招领处'),
(2, '学生证', 0, '证件', '食堂', '13800138000', 1, 1, '蓝色学生证，姓名李明'),
(3, '雨伞', 1, '衣物', '宿舍楼前', '13800138001', 2, 1, '黑色折叠伞，可联系归还');

-- ============================================
-- 创建视图 - 物品统计视图(可选)
-- ============================================
CREATE OR REPLACE VIEW `v_item_stats` AS
SELECT 
  DATE(create_time) AS `date`,
  type,
  COUNT(*) AS `count`
FROM `lost_found_item`
WHERE status = 1
GROUP BY DATE(create_time), type;

-- ============================================
-- 完成初始化
-- ============================================
COMMIT;
SELECT '✅ 数据库初始化完成！' AS status;