# 校园失物招领系统 - 运维手册 (Operations Manual)

**编制日期**: 2026-04-21  
**系统版本**: v1.0.0  
**有效期**: 正式上线至系统退役  
**目标用户**: 运维工程师、系统管理员

---

## 📋 目录

1. [系统概述](#系统概述)
2. [基础设施要求](#基础设施要求)
3. [部署指南](#部署指南)
4. [日常运维](#日常运维)
5. [性能管理](#性能管理)
6. [备份与恢复](#备份与恢复)
7. [监控告警](#监控告警)
8. [故障排查](#故障排查)

---

## 🎯 系统概述

### 系统架构
```
用户  →  前端 (Nginx)  →  后端 (Java)  →  数据库 (MySQL)
         8081         8082              3306
         Vue.js       Spring Boot 3.1   MySQL 8.x
```

### 系统组件
| 组件 | 技术栈 | 版本 | 端口 | 备注 |
|------|--------|------|------|------|
| 前端 | Vue.js 3 | 3.3+ | 8081 | npm serve 或 Nginx |
| 后端 | Spring Boot | 3.1.10 | 8082 | Java 17, JAR 启动 |
| 数据库 | MySQL | 8.0+ | 3306 | campus_db 数据库 |

### 关键指标
- **并发用户**: 100+ (本地测试)
- **QPS**: 1000+ (本地测试)
- **P95 响应时间**: < 50ms (实际 12-38ms)
- **可用性目标**: 99.9% (每月最多 43 分钟故障时间)

---

## 🔧 基础设施要求

### 服务器配置 (生产环境推荐)
```
CPU:        2 核心以上 (建议 4 核)
内存:       4GB 以上 (建议 8GB)
磁盘:       100GB SSD (至少 50GB 可用)
网络:       5Mbps 带宽 (至少 1Mbps)
操作系统:   Linux (CentOS 7.x / Ubuntu 18.04+) 或 Windows Server
```

### 数据库配置 (MySQL)
```
版本:       8.0 以上
字符集:     utf8mb4
最大连接数: 100
最大查询时间: 30s
```

### 依赖软件
- **Java Runtime**: JDK 17.0.12 以上
- **Nginx**: 1.18+ (如使用反向代理)
- **MySQL Client**: 8.0+ (用于数据库管理)

---

## 🚀 部署指南

### 第 1 步: 环境准备

#### 1.1 安装 Java 17
```bash
# CentOS/RHEL
yum install java-17-openjdk-devel

# Ubuntu/Debian
apt-get install openjdk-17-jdk

# 验证安装
java -version
```

#### 1.2 安装 MySQL 8.0
```bash
# CentOS/RHEL
yum install mysql-server

# Ubuntu/Debian
apt-get install mysql-server

# 启动服务
systemctl start mysql
systemctl enable mysql
```

#### 1.3 创建数据库与用户
```sql
-- 创建数据库
CREATE DATABASE campus_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户
CREATE USER 'campus_user'@'localhost' IDENTIFIED BY 'secure_password_here';

-- 授予权限
GRANT ALL PRIVILEGES ON campus_db.* TO 'campus_user'@'localhost';
FLUSH PRIVILEGES;
```

#### 1.4 导入数据库脚本
```bash
# 从文件导入
mysql -u campus_user -p campus_db < /path/to/campus_db.sql

# 验证导入
mysql -u campus_user -p campus_db -e "SHOW TABLES;"
```

### 第 2 步: 后端部署

#### 2.1 上传 JAR 文件
```bash
# 在服务器创建应用目录
mkdir -p /opt/campus-backend
cd /opt/campus-backend

# 上传 JAR 文件 (使用 SCP 或其他方式)
scp campus-backend-0.0.1-SNAPSHOT.jar user@server:/opt/campus-backend/
```

#### 2.2 配置应用参数
```bash
# 创建生产环境配置文件
cat > /opt/campus-backend/application-prod.yml << EOF
server:
  port: 8082
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/campus_db?useUnicode=true&characterEncoding=utf-8
    username: campus_user
    password: secure_password_here
  jpa:
    open-in-view: false
management:
  endpoints:
    web:
      exposure:
        include: health,info
EOF
```

#### 2.3 启动后端应用
```bash
# 前台运行 (用于测试)
java -jar campus-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# 后台运行 (生产环境)
nohup java -Xms512m -Xmx1024m -jar campus-backend-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod > logs/app.log 2>&1 &

# 保存进程 ID (用于停止应用)
echo $! > app.pid
```

#### 2.4 验证后端启动
```bash
# 检查进程
ps aux | grep java

# 检查端口
netstat -tulpn | grep 8082

# 健康检查
curl http://localhost:8082/actuator/health
```

### 第 3 步: 前端部署

#### 3.1 编译前端代码
```bash
# 在开发机器上执行
cd campus-frontend
npm install
npm run build

# 输出文件在 dist 目录
ls dist/
```

#### 3.2 部署到 Nginx
```bash
# 创建 Nginx 配置
cat > /etc/nginx/sites-available/campus-frontend << 'EOF'
server {
    listen 8081;
    server_name _;
    
    root /usr/share/nginx/html/campus;
    index index.html;
    
    # 反向代理后端 API
    location /api {
        proxy_pass http://127.0.0.1:8082;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    # 健康检查
    location /actuator {
        proxy_pass http://127.0.0.1:8082;
    }
}
EOF

# 启用配置
ln -s /etc/nginx/sites-available/campus-frontend /etc/nginx/sites-enabled/

# 测试配置
nginx -t

# 重启 Nginx
systemctl restart nginx
```

#### 3.3 上传前端文件
```bash
# 创建前端目录
mkdir -p /usr/share/nginx/html/campus

# 上传 dist 文件
scp -r dist/* user@server:/usr/share/nginx/html/campus/

# 验证文件
curl http://localhost:8081/
```

---

## 📅 日常运维

### 日常检查 (每日)
```bash
#!/bin/bash
# daily_check.sh - 每日健康检查脚本

echo "=== Daily Health Check ==="

# 1. 检查后端服务
echo "Checking backend service..."
curl -s http://localhost:8082/actuator/health | grep -q "UP"
if [ $? -eq 0 ]; then
    echo "[OK] Backend is running"
else
    echo "[WARN] Backend is DOWN"
fi

# 2. 检查前端服务
echo "Checking frontend service..."
curl -s http://localhost:8081/ | grep -q "html"
if [ $? -eq 0 ]; then
    echo "[OK] Frontend is running"
else
    echo "[WARN] Frontend is DOWN"
fi

# 3. 检查数据库
echo "Checking database..."
mysql -u campus_user -ppassword -e "SELECT 1;" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "[OK] Database is running"
else
    echo "[WARN] Database is DOWN"
fi

# 4. 检查磁盘使用
echo "Checking disk usage..."
DISK_USAGE=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
if [ $DISK_USAGE -gt 90 ]; then
    echo "[WARN] Disk usage is ${DISK_USAGE}%"
else
    echo "[OK] Disk usage is ${DISK_USAGE}%"
fi

echo "=== Check Complete ==="
```

### 定期维护任务

#### 周任务 (每周一)
- [ ] 查看应用错误日志，统计异常趋势
- [ ] 检查数据库备份是否完成
- [ ] 清理临时文件和过期日志
- [ ] 验证 HTTPS 证书有效期

#### 月任务 (每月初)
- [ ] 性能基线采样 (响应时间、QPS、CPU、内存)
- [ ] 数据库增长趋势分析
- [ ] 审计访问日志，查看是否有异常请求
- [ ] 验证备份的可恢复性 (恢复到测试环境)

#### 季任务 (每季度初)
- [ ] 依赖库安全更新检查 (Maven, npm 包)
- [ ] 代码静态分析和安全扫描
- [ ] 容量规划评估 (是否需要扩容)
- [ ] 灾难恢复演练

---

## 📊 性能管理

### 性能监控指标

#### 关键指标
| 指标 | 告警阈值 | 备注 |
|------|---------|------|
| CPU 使用率 | > 80% | 持续 5 分钟 |
| 内存使用率 | > 85% | 持续 10 分钟 |
| 磁盘使用率 | > 90% | 立即告警 |
| API 错误率 | > 1% | 每小时 |
| API P95 响应时间 | > 500ms | 每 5 分钟 |

#### 监控采样命令
```bash
# CPU 和内存
top -bn1 | grep "Cpu\|Mem"

# 磁盘使用
df -h

# 网络流量
iftop -n -s 5

# 数据库连接数
mysql -u root -p -e "SHOW PROCESSLIST;" | wc -l

# 应用 QPS
tail -f logs/app.log | grep "request" | wc -l
```

### 性能优化建议

#### 如果 P95 响应时间 > 100ms
1. 检查数据库索引 (EXPLAIN 分析慢查询)
2. 增加缓存 (Redis)
3. 考虑数据库读写分离

#### 如果 CPU 使用率 > 80%
1. 检查是否有 CPU 密集操作
2. 优化 GC 参数
3. 考虑增加服务器 CPU

#### 如果内存使用率 > 85%
1. 检查是否有内存泄漏
2. 优化堆大小
3. 考虑增加服务器内存

---

## 💾 备份与恢复

### 数据库备份策略

#### 全量备份脚本 (每天 2 次: 凌晨 2 点、下午 2 点)
```bash
#!/bin/bash
# backup_db.sh

BACKUP_DIR="/var/backups/campus-db"
DB_USER="campus_user"
DB_PASS="secure_password_here"
DB_NAME="campus_db"
BACKUP_FILE="$BACKUP_DIR/campus_db_$(date +%Y%m%d_%H%M%S).sql"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 执行备份
mysqldump -u $DB_USER -p$DB_PASS $DB_NAME > $BACKUP_FILE

# 压缩备份文件
gzip $BACKUP_FILE

# 删除 7 天前的备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete

echo "Backup completed: $BACKUP_FILE.gz"
```

#### 定时任务配置 (crontab)
```bash
# 编辑 crontab
crontab -e

# 添加以下两行
0 2 * * * /opt/scripts/backup_db.sh   # 凌晨 2 点
0 14 * * * /opt/scripts/backup_db.sh  # 下午 2 点
```

### 数据库恢复

#### 快速恢复 (停机时间最少)
```bash
# 1. 停止后端应用
pkill -f "campus-backend"

# 2. 恢复数据库
mysql -u campus_user -p campus_db < backup_file.sql

# 3. 重启后端应用
nohup java -jar campus-backend-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod > logs/app.log 2>&1 &

# 4. 验证恢复
curl http://localhost:8082/actuator/health
```

### 数据库同步 (主从配置)

#### 主服务器配置
```sql
-- 编辑 my.cnf
[mysqld]
server-id = 1
log-bin = mysql-bin
binlog-do-db = campus_db
```

#### 从服务器配置
```sql
-- 编辑 my.cnf
[mysqld]
server-id = 2
relay-log = mysql-relay-bin
relay-log-index = mysql-relay-bin.index

-- 配置主从复制
CHANGE MASTER TO
  MASTER_HOST='主服务器IP',
  MASTER_USER='sync_user',
  MASTER_PASSWORD='password',
  MASTER_LOG_FILE='mysql-bin.000001',
  MASTER_LOG_POS=154;

START SLAVE;
```

---

## 🚨 监控告警

### Prometheus + Grafana 配置 (可选)

#### Prometheus 配置 (prometheus.yml)
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'campus-backend'
    static_configs:
      - targets: ['localhost:8082']
    metrics_path: '/actuator/prometheus'
```

#### 告警规则 (alert_rules.yml)
```yaml
groups:
  - name: campus-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.01
        for: 5m
        
      - alert: HighLatency
        expr: histogram_quantile(0.95, http_request_duration_seconds) > 0.5
        for: 5m
```

---

## 🔧 故障排查

### 常见故障与解决方案

#### 故障 1: 后端无法启动
```
错误信息: "bind exception" 或 "Connection refused"

排查步骤:
1. 检查端口是否被占用: lsof -i :8082
2. 检查数据库连接: mysql -u campus_user -p -e "SELECT 1;"
3. 查看启动日志: tail -50 logs/app.log
4. 重新启动: pkill -f java && sleep 5 && startup.sh
```

#### 故障 2: 前端页面白屏
```
错误信息: 控制台 JavaScript 错误，无法连接后端 API

排查步骤:
1. 检查后端服务: curl http://localhost:8082/api/items
2. 检查前端网络请求: 浏览器开发者工具 → Network 标签
3. 查看 CORS 配置: grep -r "CORS" campus-backend/src/
4. 重新构建前端: npm run build && npm run serve
```

#### 故障 3: 数据库查询超时
```
错误信息: "Query timeout after 30s"

排查步骤:
1. 查看慢查询日志: tail -100 /var/log/mysql/slow.log
2. 分析问题查询: EXPLAIN SELECT ... 
3. 检查缺失索引: SHOW INDEX FROM table_name;
4. 创建必要索引: CREATE INDEX idx_name ON table(column);
5. 优化查询条件或重写 SQL
```

#### 故障 4: 磁盘满导致应用停止
```
错误信息: "No space left on device"

排查步骤:
1. 检查磁盘使用: df -h
2. 找出大文件: du -sh /* | sort -rh
3. 清理日志: rm -rf logs/*.log.*
4. 清理备份: rm -f /var/backups/*.sql.gz (保留最近 7 天)
5. 考虑扩容: 添加新磁盘或扩展分区
```

---

## 📝 变更管理

### 部署变更清单
```
变更类型: [ ] 缺陷修复 [ ] 功能新增 [ ] 性能优化 [ ] 安全补丁
变更编号: ________________
变更描述: ________________
影响范围: [ ] 后端 [ ] 前端 [ ] 数据库
风险等级: [ ] 低 [ ] 中 [ ] 高
回滚方案: ________________

部署前检查:
[ ] 代码审查通过
[ ] 测试通过 (单元测试、集成测试)
[ ] 数据库备份完成
[ ] 回滚版本已准备

部署后验证:
[ ] 应用启动正常
[ ] 健康检查通过
[ ] 核心功能可用
[ ] 性能指标正常
[ ] 错误日志无异常
```

---

## 📞 应急联系方式

| 角色 | 姓名 | 电话 | 邮件 | 可用时间 |
|------|------|------|------|---------|
| 技术负责人 | [待确认] | [待确认] | [待确认] | 工作日 9-18 |
| 数据库管理员 | [待确认] | [待确认] | [待确认] | 工作日 9-18 |
| 系统管理员 | [待确认] | [待确认] | [待确认] | 24/7 |

**应急流程**: 无法自行解决 → 一级支持 → 二级支持 → 技术负责人

---

**文档完成日期**: 2026-04-21  
**版本**: v1.0.0  
**下一次更新**: 2026-05-07 (上线后评审)
