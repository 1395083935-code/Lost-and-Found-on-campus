-- 简化的数据库优化脚本（逐步执行）
USE campus_db;

-- 注：nickname列已存在，跳过添加步骤

-- 创建关键索引
CREATE INDEX idx_username ON user(username);
CREATE INDEX idx_student_id ON user(student_id);
CREATE INDEX idx_status ON user(status);

-- LostFoundItem关键索引
CREATE INDEX idx_item_status_time ON lost_found_item(status, create_time DESC);
CREATE INDEX idx_item_user_id ON lost_found_item(user_id);
CREATE INDEX idx_item_create_time ON lost_found_item(create_time DESC);
CREATE INDEX idx_item_title ON lost_found_item(title);

-- Report关键索引
CREATE INDEX idx_report_status_time ON report(status, create_time DESC);
CREATE INDEX idx_report_user_id ON report(report_user_id);

-- SearchHotwords关键索引
CREATE INDEX idx_search_count_desc ON search_hotwords(search_count DESC);
CREATE INDEX idx_last_search_time_desc ON search_hotwords(last_search_time DESC);

-- Favorite关键索引
CREATE INDEX idx_fav_user_time ON favorite(user_id, create_time DESC);
CREATE INDEX idx_fav_item_id ON favorite(item_id);

-- Notice关键索引
CREATE INDEX idx_notice_user_time ON notice(user_id, create_time DESC);
CREATE INDEX idx_notice_status ON notice(status);

-- 显示优化结果
SELECT 'INDEX OPTIMIZATION COMPLETED' as status;
