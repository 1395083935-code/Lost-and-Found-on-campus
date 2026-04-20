# 校园失物招领系统 - MySQL 数据库初始化指南

**版本**: v1.0.0  
**生效时间**: 2026-05-01 (在 PRODUCTION_SERVER_SETUP.md 之后)  
**目标环境**: 生产 MySQL 8.0+  
**预计时间**: 1-2 小时

---

## 📋 前置条件

### 确认已完成
- [x] 生产服务器已部署 (见 PRODUCTION_SERVER_SETUP.md)
- [x] 拥有 campus_db.sql 初始化脚本
- [x] MySQL 8.0 已安装在服务器上

### 数据库要求
```
MySQL 版本:    8.0+
字符集:        utf8mb4 (支持 emoji 等特殊字符)
端口:         3306
最大连接数:    200+
```

---

## 🔧 第 1 步: MySQL 服务启动 (5 分钟)

### 1.1 安装 MySQL Server

**CentOS 8.x**:
```bash
# 安装 MySQL 8.0
sudo yum install -y mysql-server

# 启动服务
sudo systemctl start mysqld
sudo systemctl enable mysqld  # 开机自启

# 验证
sudo systemctl status mysqld
```

**Ubuntu 20.04**:
```bash
# 安装 MySQL 8.0
sudo apt install -y mysql-server

# 启动服务
sudo systemctl start mysql
sudo systemctl enable mysql  # 开机自启

# 验证
sudo systemctl status mysql
```

### 1.2 初始化 MySQL (仅首次安装)

```bash
# CentOS: MySQL 已自动初始化
# Ubuntu: 运行安装后脚本
sudo mysql_secure_installation

# 交互式提示:
# - 输入 root 密码 (留空)
# - 是否设置 root 密码? [Y/n] Y
# - 输入新密码: [强密码]
# - 是否删除匿名用户? [Y/n] Y
# - 是否禁用 root 远程登录? [Y/n] Y
# - 是否删除 test 数据库? [Y/n] Y
# - 是否立即重载权限表? [Y/n] Y
```

### 1.3 验证连接

```bash
# 连接到 MySQL (使用 root 账户)
mysql -u root -p

# 输入密码后看到 mysql> 提示符即成功
# 退出: exit;
```

---

## 📊 第 2 步: 创建应用数据库和账户 (10 分钟)

### 2.1 创建数据库

```bash
# 连接到 MySQL
mysql -u root -p

# 执行以下 SQL (粘贴进 mysql 命令行)
```

```sql
-- 创建数据库 (utf8mb4 字符集支持 emoji)
CREATE DATABASE campus_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 验证
SHOW DATABASES;
-- 输出应该包含 campus_db
```

### 2.2 创建应用用户和权限

```sql
-- 创建应用用户 (生产环境)
CREATE USER 'campus_user'@'localhost' IDENTIFIED BY 'Strong_Password_123!';

-- 授予权限 (仅限本地连接)
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, LOCK TABLES 
ON campus_db.* 
TO 'campus_user'@'localhost';

-- 刷新权限
FLUSH PRIVILEGES;

-- 验证
SELECT user, host FROM mysql.user WHERE user='campus_user';
SHOW GRANTS FOR 'campus_user'@'localhost';
```

### 2.3 创建备份用户 (可选但推荐)

```sql
-- 创建备份用户 (只读权限)
CREATE USER 'campus_backup'@'localhost' IDENTIFIED BY 'Backup_Password_456!';

-- 授予备份权限
GRANT SELECT, LOCK TABLES, RELOAD 
ON *.* 
TO 'campus_backup'@'localhost';

FLUSH PRIVILEGES;
```

### 2.4 验证用户创建

```bash
# 使用新用户连接
mysql -u campus_user -p campus_db

# 输入密码 (Strong_Password_123!)
# 看到 mysql> 提示符即成功

# 验证数据库为空
SHOW TABLES;
# Empty set (0 rows in set)

# 退出
exit;
```

---

## 📥 第 3 步: 导入初始化脚本 (15 分钟)

### 3.1 上传初始化脚本

```bash
# 从本地上传到服务器
scp campus_db.sql root@your-server:/tmp/

# 或使用 sftp:
# sftp root@your-server
# > put campus_db.sql /tmp/
# > quit
```

### 3.2 导入数据库

```bash
# 方法 1: 使用 mysql 命令行 (推荐用于大文件)
mysql -u campus_user -p campus_db < /tmp/campus_db.sql

# 输入密码后等待导入完成
# 根据文件大小，可能需要 30 秒 - 2 分钟

# 方法 2: 在 MySQL 交互式下导入
mysql -u campus_user -p campus_db
mysql> SOURCE /tmp/campus_db.sql;
```

### 3.3 验证数据导入

```bash
# 连接到数据库
mysql -u campus_user -p campus_db

# 检查表数量
SHOW TABLES;
# 应该看到:
# - user
# - lost_found_item
# - report
# - search_hotwords
# - notice
# - favorite
# - admin
# - 其他表...

# 检查数据量
SELECT COUNT(*) as 'Total Users' FROM user;
SELECT COUNT(*) as 'Total Items' FROM lost_found_item;
SELECT COUNT(*) as 'Total Hotwords' FROM search_hotwords;

# 查看表结构
DESCRIBE user;
# 应该看到: id, username, password, email, avatar, created_at, updated_at

# 验证索引
SHOW INDEX FROM lost_found_item;
# 应该看到多个索引已创建
```

---

## 🔐 第 4 步: 安全性配置 (15 分钟)

### 4.1 禁用不必要的用户

```sql
-- 查看所有用户
SELECT user, host FROM mysql.user;

-- 删除默认用户 (如果存在)
DROP USER ''@'localhost';       -- 匿名用户
DROP USER ''@'your-hostname';   -- 另一个匿名用户
DROP USER 'root'@'127.0.0.1';   -- 如果有多个 root 账户，保留一个

-- 仅保留:
-- root (用于管理)
-- campus_user (应用账户)
-- campus_backup (备份账户)
```

### 4.2 配置 MySQL 参数优化

**编辑 MySQL 配置** (`/etc/mysql/mysql.conf.d/mysqld.cnf` 或 `/etc/my.cnf`):
```ini
[mysqld]
# 字符集配置
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci

# 最大连接数
max_connections=200

# 查询超时 (防止长查询)
max_execution_time=30000

# 连接超时
wait_timeout=600
interactive_timeout=600

# 缓冲池大小 (根据服务器内存调整，这里假设 4GB)
innodb_buffer_pool_size=2G

# 日志配置
log_error=/var/log/mysql/error.log
slow_query_log=1
slow_query_log_file=/var/log/mysql/slow-query.log
long_query_time=2

# 备份相关
binlog_format=ROW  # 用于主从复制和备份
log_bin=/var/log/mysql/mysql-bin.log
```

### 4.3 应用配置更新

```bash
# 重启 MySQL
sudo systemctl restart mysqld

# 验证配置
mysql -u root -p
mysql> SHOW VARIABLES LIKE 'max_connections';
# 应该显示 200 或您设置的值
mysql> SHOW VARIABLES LIKE 'character_set_server';
# 应该显示 utf8mb4
```

---

## 💾 第 5 步: 备份策略设置 (20 分钟)

### 5.1 创建备份目录

```bash
# 创建备份目录
sudo mkdir -p /var/backup/mysql
sudo chown mysql:mysql /var/backup/mysql
sudo chmod 750 /var/backup/mysql

# 创建日志目录
sudo mkdir -p /var/log/backup
sudo chown mysql:mysql /var/log/backup
sudo chmod 750 /var/log/backup
```

### 5.2 创建全量备份脚本

**创建备份脚本** (`/usr/local/bin/campus_backup_full.sh`):
```bash
#!/bin/bash

# Campus Full Database Backup Script

BACKUP_DIR="/var/backup/mysql"
LOG_DIR="/var/log/backup"
DB_USER="campus_backup"
DB_PASS="Backup_Password_456!"
DB_NAME="campus_db"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/campus_db_full_$DATE.sql"
LOG_FILE="$LOG_DIR/backup_$DATE.log"

# 创建备份
echo "[$(date)] Starting full backup..." >> $LOG_FILE

mysqldump \
    -u $DB_USER -p$DB_PASS \
    --single-transaction \
    --quick \
    --lock-tables=false \
    --all-databases \
    > $BACKUP_FILE 2>> $LOG_FILE

if [ $? -eq 0 ]; then
    # 压缩备份
    gzip $BACKUP_FILE
    BACKUP_FILE="${BACKUP_FILE}.gz"
    
    # 记录大小
    SIZE=$(du -h $BACKUP_FILE | awk '{print $1}')
    echo "[$(date)] ✅ Backup completed. Size: $SIZE" >> $LOG_FILE
    
    # 清理 7 天前的备份
    find $BACKUP_DIR -name "campus_db_full_*.sql.gz" -mtime +7 -delete
    echo "[$(date)] Old backups cleaned up" >> $LOG_FILE
else
    echo "[$(date)] ❌ Backup failed!" >> $LOG_FILE
    exit 1
fi
```

**设置可执行权限**:
```bash
sudo chmod +x /usr/local/bin/campus_backup_full.sh
```

### 5.3 创建增量备份脚本 (用于 binlog)

**创建增量备份脚本** (`/usr/local/bin/campus_backup_incremental.sh`):
```bash
#!/bin/bash

# Campus Incremental Backup Script (based on binlog)

BACKUP_DIR="/var/backup/mysql"
LOG_DIR="/var/log/backup"
BINLOG_DIR="/var/log/mysql"
DB_USER="campus_backup"
DB_PASS="Backup_Password_456!"

# 刷新二进制日志 (创建新的日志文件)
mysql -u $DB_USER -p$DB_PASS -e "FLUSH LOGS;"

# 获取最新的 binlog 文件
LATEST_BINLOG=$(ls -t $BINLOG_DIR/mysql-bin.* 2>/dev/null | head -1)

if [ -f "$LATEST_BINLOG" ]; then
    # 复制 binlog 到备份目录
    cp $LATEST_BINLOG $BACKUP_DIR/
    
    # 压缩
    gzip $BACKUP_DIR/$(basename $LATEST_BINLOG)
    
    echo "[$(date)] ✅ Incremental backup created" >> $LOG_DIR/incremental_$(\date +%Y%m%d).log
else
    echo "[$(date)] ⚠️  No binlog found" >> $LOG_DIR/incremental_$(\date +%Y%m%d).log
fi
```

**设置可执行权限**:
```bash
sudo chmod +x /usr/local/bin/campus_backup_incremental.sh
```

### 5.4 设置自动化备份任务

**编辑 crontab**:
```bash
sudo crontab -e

# 添加以下行:
# 每天 00:00 执行全量备份
0 0 * * * /usr/local/bin/campus_backup_full.sh >> /var/log/backup/cron.log 2>&1

# 每 6 小时执行一次增量备份
0 */6 * * * /usr/local/bin/campus_backup_incremental.sh >> /var/log/backup/cron.log 2>&1
```

### 5.5 验证备份

```bash
# 检查备份文件
ls -lh /var/backup/mysql/
# -rw-r--r-- 1 mysql mysql 2.5M May  1 10:00 campus_db_full_20260501_100000.sql.gz

# 检查备份大小 (应该大于 500KB)
du -h /var/backup/mysql/

# 测试恢复 (可选，在测试环境)
gunzip -c /var/backup/mysql/campus_db_full_*.sql.gz | mysql -u root -p
```

---

## 🔄 第 6 步: 备份恢复测试 (15 分钟)

### 6.1 恢复过程 (在生产环境之前必须测试)

**完整恢复**:
```bash
# 步骤 1: 停止应用 (如果已在运行)
sudo systemctl stop campus-backend.service

# 步骤 2: 备份当前数据库 (以防万一)
mysqldump -u campus_user -p campus_db | gzip > /tmp/campus_db_backup.sql.gz

# 步骤 3: 删除现有数据库 (仅在测试环境)
mysql -u root -p
mysql> DROP DATABASE campus_db;
mysql> exit;

# 步骤 4: 恢复备份
gunzip < /var/backup/mysql/campus_db_full_*.sql.gz | mysql -u root -p

# 步骤 5: 验证恢复
mysql -u campus_user -p campus_db
mysql> SELECT COUNT(*) FROM user;
mysql> SELECT COUNT(*) FROM lost_found_item;
mysql> exit;

# 步骤 6: 重启应用
sudo systemctl start campus-backend.service

# 步骤 7: 验证应用
curl http://localhost:8082/actuator/health
```

### 6.2 部分恢复 (恢复单个表)

```bash
# 从备份中提取单个表
gunzip < /var/backup/mysql/campus_db_full_*.sql.gz | mysql -u root -p -e "
    USE campus_db;
    -- 恢复 user 表
    SOURCE /path/to/user_table.sql;
"
```

---

## 🔍 第 7 步: 性能监控和优化 (15 分钟)

### 7.1 查看数据库性能指标

```bash
# 连接到 MySQL
mysql -u root -p

# 查看当前连接数
SHOW PROCESSLIST;

# 查看慢查询日志
SHOW VARIABLES LIKE 'slow_query_log';

# 查看查询缓存 (MySQL 8.0 已移除，但仍可查看)
SHOW VARIABLES LIKE 'query_cache_%';

# 查看最大连接数
SHOW VARIABLES LIKE 'max_connections';

# 查看当前打开的表数
SHOW OPEN TABLES;
```

### 7.2 优化常用查询

```sql
-- 查看表大小
SELECT 
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) as 'Size (MB)'
FROM information_schema.TABLES
WHERE table_schema = 'campus_db'
ORDER BY (data_length + index_length) DESC;

-- 查看索引情况
SELECT 
    table_name,
    index_name,
    seq_in_index,
    column_name
FROM information_schema.STATISTICS
WHERE table_schema = 'campus_db'
ORDER BY table_name, index_name, seq_in_index;

-- 查看表的创建时间和更新时间
SELECT 
    table_name,
    create_time,
    update_time
FROM information_schema.TABLES
WHERE table_schema = 'campus_db';
```

---

## ✅ 数据库部署验证清单

| 检查项 | 命令 | 预期结果 | 完成 |
|--------|------|---------|------|
| MySQL 服务 | `sudo systemctl status mysqld` | Active (running) | ☐ |
| 数据库创建 | `SHOW DATABASES;` | 包含 campus_db | ☐ |
| 用户创建 | `SELECT user, host FROM mysql.user;` | campus_user@localhost | ☐ |
| 权限授予 | `SHOW GRANTS FOR 'campus_user'@'localhost';` | SELECT, INSERT, ... | ☐ |
| 表导入 | `SHOW TABLES;` | 8-10 个表 | ☐ |
| 数据导入 | `SELECT COUNT(*) FROM user;` | 6+ 行 | ☐ |
| 字符集 | `SHOW VARIABLES LIKE 'character_set_server';` | utf8mb4 | ☐ |
| 连接数 | `SHOW VARIABLES LIKE 'max_connections';` | 200+ | ☐ |
| 备份脚本 | `ls -l /usr/local/bin/campus_backup_*.sh` | 2 个脚本 | ☐ |
| 备份任务 | `sudo crontab -l \| grep campus` | 2 个 cron 任务 | ☐ |

---

## 🚨 常见问题排查

### 导入脚本失败
```bash
# 问题: Access denied for user 'campus_user'
# 解决:
# 1. 确保用户已创建: SHOW GRANTS FOR 'campus_user'@'localhost';
# 2. 使用 root 导入: mysql -u root -p campus_db < campus_db.sql

# 问题: Unknown character set 'utf8mb4'
# 解决:
mysql -u root -p
mysql> SET NAMES utf8mb4;
```

### 备份失败
```bash
# 问题: mysqldump: Got error: 1251 when running CREATE ALGORITHM=UNDEFINED
# 解决:
# 在备份命令中添加: --skip-definer

# 问题: Permission denied when writing to /var/backup
# 解决:
sudo chown mysql:mysql /var/backup/mysql
sudo chmod 750 /var/backup/mysql
```

### 连接问题
```bash
# 问题: Can't connect to MySQL server
# 解决:
# 1. 检查 MySQL 是否运行: sudo systemctl status mysqld
# 2. 检查端口是否开放: netstat -tuln | grep 3306
# 3. 检查防火墙规则: sudo firewall-cmd --list-all
```

---

## 📝 数据库部署确认

| 人员 | 确认事项 | 日期 | 签名 |
|------|---------|------|------|
| DBA | 数据库创建、用户设置、权限配置 | | |
| 开发 | 数据导入、应用连接测试 | | |
| 运维 | 备份任务、监控配置 | | |

---

**预计完成时间**: 2026-05-01 下午 15:00  
**下一步**: HTTPS 证书配置 (参考 PRODUCTION_HTTPS_SETUP.md)
