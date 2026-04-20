-- ============================================
-- P2-4 搜索性能优化 - 数据库迁移脚本
-- 添加全文索引支持大规模搜索
-- ============================================

-- 1. 为lost_found_item表添加全文索引（支持中文）
-- 注意：需要MySQL版本 >= 5.7，并支持ngram分词

-- 删除旧的全文索引（如果存在）
ALTER TABLE `lost_found_item` DROP INDEX IF EXISTS `ft_search_items`;

-- 创建全文索引（使用ngram分词器支持中文）
ALTER TABLE `lost_found_item` ADD FULLTEXT INDEX `ft_search_items` (
  `title`,
  `description`,
  `location`,
  `category`
) WITH PARSER ngram;

-- 2. 创建搜索关键词热度统计表
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

-- 3. 为用户表添加nickname字段（如果尚未存在）
ALTER TABLE `user` ADD COLUMN `nickname` VARCHAR(255) COMMENT '用户昵称（微信昵称或自设置）' AFTER `avatar`;

-- 4. 添加优化索引：status和create_time联合索引（用于首页列表过滤）
ALTER TABLE `lost_found_item` ADD INDEX IF NOT EXISTS `idx_status_create_time` (`status`, `create_time` DESC);

-- 5. 添加优化索引：type和status联合索引（用于分类筛选）
ALTER TABLE `lost_found_item` ADD INDEX IF NOT EXISTS `idx_type_status` (`type`, `status`);

-- 设置ngram分词器配置（可选，如需调整分词粒度）
-- 全文索引配置说明：
-- - ngram分词器: 适合中文，将连续的字符分成多个词
-- - 默认分词长度: 2个字符（可通过 ft_min_word_len 调整）
-- - MySQL 5.7+ 内置ngram支持
-- - 性能：全文索引查询比LIKE快100倍以上

-- ============================================
-- 验证脚本（可选，用于检查索引创建情况）
-- ============================================
-- SHOW INDEX FROM `lost_found_item`;
-- SELECT * FROM INFORMATION_SCHEMA.STATISTICS 
-- WHERE TABLE_NAME='lost_found_item' AND INDEX_TYPE='FULLTEXT';
