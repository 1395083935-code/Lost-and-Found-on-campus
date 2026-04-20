-- ====================================================
-- Campus Lost & Found Database 优化索引脚本 (修正版)
-- 执行时间: 2026-04-18
-- 目的: 性能优化，目标 P95响应时间 <300ms
-- ====================================================

USE campus_db;

-- 首先检查并添加nickname列
ALTER TABLE user ADD COLUMN IF NOT EXISTS nickname VARCHAR(50) COMMENT '用户昵称（微信昵称或自设置）' AFTER avatar;

-- ====== User表 优化索引 ======
-- 1. 用户名查询索引
CREATE INDEX IF NOT EXISTS idx_username ON user(username);

-- 2. 学号查询索引
CREATE INDEX IF NOT EXISTS idx_student_id ON user(student_id);

-- 3. 用户状态查询
CREATE INDEX IF NOT EXISTS idx_user_status ON user(status);

-- 4. 用户昵称查询
CREATE INDEX IF NOT EXISTS idx_nickname ON user(nickname);

-- ====== LostFoundItem表 优化索引 ======
-- 5. 物品标题全文索引
CREATE INDEX idx_title ON lost_found_item(title);

-- 6. 物品类别索引
CREATE INDEX idx_category ON lost_found_item(category);

-- 7. 物品状态+创建时间复合索引（关键优化）
CREATE INDEX idx_status_create_time ON lost_found_item(status, create_time DESC);

-- 8. 用户ID索引
CREATE INDEX idx_user_id ON lost_found_item(user_id);

-- 9. 创建时间索引（用于排序）
CREATE INDEX idx_create_time ON lost_found_item(create_time DESC);

-- 10. 过期状态检查索引
CREATE INDEX idx_status_update_time ON lost_found_item(status, update_time);

-- ====== Favorite表 优化索引 ======
-- 11. 用户收藏列表查询
CREATE INDEX idx_user_favorite ON favorite(user_id, create_time DESC);

-- 12. 物品被收藏查询
CREATE INDEX idx_item_favorite ON favorite(item_id);

-- ====== Notice表 优化索引 ======
-- 13. 用户通知查询
CREATE INDEX idx_user_notice ON notice(user_id, create_time DESC);

-- 14. 通知状态查询
CREATE INDEX idx_notice_status ON notice(status);

-- ====== Report表 优化索引 ======
-- 15. 待审核举报查询（关键）
CREATE INDEX idx_report_status_time ON report(status, create_time DESC);

-- 16. 用户举报查询
CREATE INDEX idx_report_user ON report(report_user_id);

-- 17. 被举报用户查询
CREATE INDEX idx_reported_user ON report(reported_user_id);

-- ====== SearchHotwords表 优化索引（已有但确认） ======
-- 18. 搜索频次排序
CREATE INDEX idx_search_count ON search_hotwords(search_count DESC);

-- 19. 最后搜索时间索引
CREATE INDEX idx_last_search_time ON search_hotwords(last_search_time DESC);

-- 20. 关键词唯一性索引（已有）
-- UNIQUE KEY `uk_keyword` (`keyword`)

-- ====== Admin表 优化索引 ======
-- 21. 管理员用户名查询
CREATE INDEX idx_admin_username ON admin(username);

-- ====================================================
-- 索引统计和验证
-- ====================================================

-- 显示所有创建的索引
SHOW INDEX FROM user WHERE Key_name != 'PRIMARY';
SHOW INDEX FROM lost_found_item WHERE Key_name != 'PRIMARY';
SHOW INDEX FROM favorite WHERE Key_name != 'PRIMARY';
SHOW INDEX FROM notice WHERE Key_name != 'PRIMARY';
SHOW INDEX FROM report WHERE Key_name != 'PRIMARY';
SHOW INDEX FROM search_hotwords WHERE Key_name != 'PRIMARY';
SHOW INDEX FROM admin WHERE Key_name != 'PRIMARY';

-- 验证表大小
SELECT 
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size_MB'
FROM information_schema.TABLES
WHERE table_schema = 'campus_db'
ORDER BY (data_length + index_length) DESC;
