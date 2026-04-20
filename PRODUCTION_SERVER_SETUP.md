# 校园失物招领系统 - 生产服务器部署指南

**版本**: v1.0.0  
**生效时间**: 2026-05-01  
**目标环境**: 生产服务器 (Linux)  
**预计时间**: 2-3 小时

---

## 📋 部署前检查清单

### 硬件要求
```
✅ CPU:        2 Core+
✅ 内存:       4 GB+
✅ 磁盘空间:   50 GB+ (留出日志和备份空间)
✅ 网络:       公网 IP，防火墙规则已开放 80/443/3306
✅ 操作系统:   CentOS 8.x 或 Ubuntu 20.04 LTS+
```

### 需要的账号与权限
```
✅ Root 或 sudo 权限账户
✅ 域名所有权 (用于 HTTPS 证书)
✅ SSH 密钥对或强密码
```

### 需要的文件
```
✅ campus-backend-0.0.1-SNAPSHOT.jar (64.8 MB)
✅ campus_db.sql (数据库初始化脚本)
✅ application.yml (应用配置文件)
✅ nginx.conf (Nginx 反向代理配置)
```

---

## 🔧 第 1 步: 操作系统环境准备 (15 分钟)

### 1.1 系统更新

**CentOS 8.x**:
```bash
sudo yum update -y
sudo yum install -y wget curl git
sudo yum install -y net-tools
```

**Ubuntu 20.04**:
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y wget curl git net-tools
```

### 1.2 创建应用目录结构

```bash
# 创建应用用户 (可选，推荐)
sudo useradd -m -s /bin/bash campus

# 创建应用目录
sudo mkdir -p /opt/campus/{app,data,logs,backup}
sudo mkdir -p /etc/campus
sudo mkdir -p /var/log/campus

# 设置权限
sudo chown -R campus:campus /opt/campus
sudo chown -R campus:campus /var/log/campus

# 创建快速启动脚本目录
sudo mkdir -p /usr/local/bin/campus
```

### 1.3 配置系统参数

```bash
# 增加文件描述符限制 (支持高并发)
sudo tee -a /etc/security/limits.conf > /dev/null <<EOF
campus soft nofile 65535
campus hard nofile 65535
campus soft nproc 32768
campus hard nproc 32768
EOF

# 加载配置
sudo sysctl -p
```

### 1.4 配置防火墙规则

**CentOS (firewalld)**:
```bash
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --permanent --add-port=8082/tcp
sudo firewall-cmd --permanent --add-port=3306/tcp
sudo firewall-cmd --reload
```

**Ubuntu (ufw)**:
```bash
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 8082/tcp
sudo ufw allow 3306/tcp
sudo ufw enable
```

---

## 🚀 第 2 步: Java 17 安装 (10 分钟)

### 2.1 安装 JDK 17

**CentOS 8.x**:
```bash
sudo yum install -y java-17-openjdk java-17-openjdk-devel
```

**Ubuntu 20.04**:
```bash
sudo apt install -y openjdk-17-jdk openjdk-17-jre

# 如果 apt 中没有 JDK 17，使用 SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 17.0.12-open
```

### 2.2 验证 Java 安装

```bash
java -version
javac -version

# 预期输出:
# openjdk version "17.0.12" 2024-07-16
# OpenJDK Runtime Environment (build 17.0.12+7-Ubuntu-0ubuntu0.20.04.1)
```

### 2.3 配置 JAVA_HOME 环境变量

```bash
# 找到 Java 安装位置
which java
# /usr/bin/java

# 获取 JAVA_HOME
readlink -f /usr/bin/java | sed "s:bin/java::"
# /usr/lib/jvm/java-17-openjdk-amd64/

# 配置环境变量
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' | sudo tee -a /etc/profile.d/java.sh
echo 'export PATH=$JAVA_HOME/bin:$PATH' | sudo tee -a /etc/profile.d/java.sh

# 立即生效
source /etc/profile.d/java.sh

# 验证
echo $JAVA_HOME
```

---

## 📦 第 3 步: Nginx 安装与配置 (15 分钟)

### 3.1 安装 Nginx

**CentOS 8.x**:
```bash
sudo yum install -y nginx
```

**Ubuntu 20.04**:
```bash
sudo apt install -y nginx
```

### 3.2 启动 Nginx

```bash
# 启动服务
sudo systemctl start nginx
sudo systemctl enable nginx  # 开机自启

# 验证
sudo systemctl status nginx
curl http://localhost
```

### 3.3 配置 Nginx 反向代理

**备份原配置**:
```bash
sudo cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.bak
```

**创建应用配置** (`/etc/nginx/conf.d/campus.conf`):
```nginx
# HTTP 重定向到 HTTPS (5/3 配置)
server {
    listen 80;
    server_name campus.edu.cn www.campus.edu.cn;
    return 301 https://$server_name$request_uri;
}

# HTTPS 配置 (证书部分在第 5 步配置)
server {
    listen 443 ssl http2;
    server_name campus.edu.cn www.campus.edu.cn;

    # SSL 证书 (5/3 配置)
    ssl_certificate /etc/nginx/ssl/campus.edu.cn.crt;
    ssl_certificate_key /etc/nginx/ssl/campus.edu.cn.key;
    
    # SSL 安全配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # 日志
    access_log /var/log/nginx/campus_access.log;
    error_log /var/log/nginx/campus_error.log;

    # 前端静态文件
    location / {
        root /usr/share/nginx/html/campus;
        try_files $uri $uri/ /index.html;
        expires 1d;
        add_header Cache-Control "public, immutable";
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://localhost:8082;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 30s;
        proxy_connect_timeout 10s;
    }

    # Actuator 端点 (监控)
    location /actuator/ {
        proxy_pass http://localhost:8082;
        proxy_set_header Host $host;
    }

    # 文档端点 (Swagger)
    location /doc.html {
        proxy_pass http://localhost:8082;
        proxy_set_header Host $host;
    }

    # 健康检查
    location = /health {
        proxy_pass http://localhost:8082/actuator/health;
        access_log off;
    }
}
```

**验证配置**:
```bash
sudo nginx -t
# 输出: nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
```

**重启 Nginx**:
```bash
sudo systemctl restart nginx
```

---

## 💾 第 4 步: 应用部署 (20 分钟)

### 4.1 准备应用文件

```bash
# 上传 JAR 文件到服务器 (本地执行)
scp -r campus-backend-0.0.1-SNAPSHOT.jar campus@your-server:/tmp/

# SSH 连接到服务器
ssh campus@your-server
```

### 4.2 部署 JAR 文件

```bash
# 检查 JAR 大小
ls -lh /tmp/campus-backend-0.0.1-SNAPSHOT.jar
# -rw-r--r--  1 campus campus 64.8M May  1 10:00 campus-backend-0.0.1-SNAPSHOT.jar

# 移动到应用目录
sudo mv /tmp/campus-backend-0.0.1-SNAPSHOT.jar /opt/campus/app/

# 创建符号链接 (方便升级)
sudo ln -s /opt/campus/app/campus-backend-0.0.1-SNAPSHOT.jar /opt/campus/app/campus-backend.jar

# 设置权限
sudo chown campus:campus /opt/campus/app/*.jar
sudo chmod 755 /opt/campus/app/*.jar
```

### 4.3 配置应用启动脚本

**创建启动脚本** (`/opt/campus/app/start.sh`):
```bash
#!/bin/bash

# Campus Backend Application Startup Script

APP_HOME=/opt/campus/app
APP_JAR=$APP_HOME/campus-backend.jar
LOG_DIR=/var/log/campus
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
PID_FILE=$APP_HOME/campus.pid

# JVM 参数
JVM_OPTS="-Xms2g -Xmx2g"           # 内存: 初始 2GB, 最大 2GB
JVM_OPTS="$JVM_OPTS -XX:+UseG1GC" # 使用 G1 垃圾收集器
JVM_OPTS="$JVM_OPTS -XX:MaxGCPauseMillis=200"

# 应用参数
APP_OPTS="--server.port=8082"
APP_OPTS="$APP_OPTS --logging.file.name=$LOG_DIR/app.log"
APP_OPTS="$APP_OPTS --logging.level.root=INFO"
APP_OPTS="$APP_OPTS --logging.level.com.campus=DEBUG"

# 启动应用
echo "Starting Campus Backend..."
nohup $JAVA_HOME/bin/java $JVM_OPTS -jar $APP_JAR $APP_OPTS >> $LOG_DIR/startup.log 2>&1 &

# 记录 PID
echo $! > $PID_FILE
echo "Campus Backend started with PID $(cat $PID_FILE)"

# 等待应用启动
sleep 5

# 检查健康状态
echo "Checking health..."
curl -s http://localhost:8082/actuator/health | grep -q "UP"
if [ $? -eq 0 ]; then
    echo "✅ Campus Backend is running!"
else
    echo "❌ Campus Backend health check failed!"
    echo "Check logs: tail -f $LOG_DIR/app.log"
    exit 1
fi
```

### 4.4 配置停止脚本

**创建停止脚本** (`/opt/campus/app/stop.sh`):
```bash
#!/bin/bash

PID_FILE=/opt/campus/app/campus.pid

if [ -f $PID_FILE ]; then
    PID=$(cat $PID_FILE)
    echo "Stopping Campus Backend (PID: $PID)..."
    
    kill $PID
    sleep 2
    
    # 如果进程仍然运行，强制杀死
    if kill -0 $PID 2>/dev/null; then
        echo "Force killing process..."
        kill -9 $PID
    fi
    
    rm -f $PID_FILE
    echo "✅ Campus Backend stopped"
else
    echo "❌ PID file not found"
    exit 1
fi
```

### 4.5 创建 Systemd 服务配置 (推荐)

**创建服务文件** (`/etc/systemd/system/campus-backend.service`):
```ini
[Unit]
Description=Campus Lost & Found Backend Service
After=network.target
Wants=network-online.target

[Service]
Type=simple
User=campus
WorkingDirectory=/opt/campus/app
ExecStart=/usr/lib/jvm/java-17-openjdk-amd64/bin/java \
  -Xms2g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -jar /opt/campus/app/campus-backend.jar \
  --server.port=8082 \
  --logging.file.name=/var/log/campus/app.log

# 自动重启
Restart=always
RestartSec=10

# 资源限制
LimitNOFILE=65535
LimitNPROC=32768

[Install]
WantedBy=multi-user.target
```

**启用服务**:
```bash
sudo systemctl daemon-reload
sudo systemctl enable campus-backend.service
sudo systemctl start campus-backend.service
sudo systemctl status campus-backend.service
```

### 4.6 验证应用启动

```bash
# 查看日志
tail -f /var/log/campus/app.log

# 检查端口监听
netstat -tuln | grep 8082
# tcp        0      0 127.0.0.1:8082         0.0.0.0:*               LISTEN

# 健康检查
curl http://localhost:8082/actuator/health
# {"status":"UP"}

# API 测试
curl http://localhost:8082/api/items
# [{"id":1,"title":"学生证",...}]
```

---

## 📊 第 5 步: 日志与监控设置 (10 分钟)

### 5.1 配置日志轮转

**创建日志配置** (`/etc/logrotate.d/campus`):
```
/var/log/campus/*.log {
    daily                 # 每天轮转
    rotate 30             # 保留 30 天
    compress              # 压缩旧日志
    delaycompress         # 延迟压缩
    notifempty            # 空文件不轮转
    create 0640 campus campus
    sharedscripts
    postrotate
        systemctl reload campus-backend.service > /dev/null 2>&1 || true
    endscript
}
```

### 5.2 监控脚本

**创建监控脚本** (`/opt/campus/scripts/monitor.sh`):
```bash
#!/bin/bash

# Campus Backend Monitoring Script

while true; do
    # 检查应用进程
    if ! pgrep -f "campus-backend.jar" > /dev/null; then
        echo "❌ [$(date)] Campus Backend is not running!"
        echo "尝试重启..."
        sudo systemctl restart campus-backend.service
        sleep 10
    fi

    # 检查健康状态
    HEALTH=$(curl -s http://localhost:8082/actuator/health | grep -o '"status":"[^"]*"')
    if [[ $HEALTH != *"UP"* ]]; then
        echo "⚠️  [$(date)] Health check warning: $HEALTH"
    fi

    # 检查磁盘空间 (日志)
    DISK_USAGE=$(df -h /var/log/campus | tail -1 | awk '{print $5}' | sed 's/%//')
    if [ $DISK_USAGE -gt 80 ]; then
        echo "⚠️  [$(date)] Disk usage is high: ${DISK_USAGE}%"
    fi

    # 检查日志大小
    LOG_SIZE=$(du -sh /var/log/campus | awk '{print $1}')
    echo "✅ [$(date)] Campus Backend is running. Log size: $LOG_SIZE"

    sleep 60
done
```

**设置定时监控**:
```bash
# 添加到 crontab (每 5 分钟检查一次)
(crontab -l 2>/dev/null; echo "*/5 * * * * /opt/campus/scripts/monitor.sh") | crontab -
```

---

## ✅ 部署验证清单

### 启动检查 (应该全部 ✅)
```bash
# 1. Java 验证
java -version
# openjdk version "17.0.12" 2024-07-16

# 2. Nginx 验证
sudo systemctl status nginx
# ● nginx.service - Nginx HTTP Server
#    Active: active (running)

# 3. 应用验证
sudo systemctl status campus-backend.service
# ● campus-backend.service - Campus Lost & Found Backend Service
#    Active: active (running)

# 4. 端口验证
netstat -tuln | grep -E ':(80|443|8082|3306)'
# tcp  0  0 0.0.0.0:80      0.0.0.0:*  LISTEN
# tcp  0  0 0.0.0.0:443     0.0.0.0:*  LISTEN
# tcp  0  0 127.0.0.1:8082  0.0.0.0:*  LISTEN

# 5. API 健康检查
curl http://localhost:8082/actuator/health
# {"status":"UP"}

# 6. 防火墙验证
sudo firewall-cmd --list-all (CentOS)
# 确认 80/tcp, 443/tcp, 8082/tcp, 3306/tcp 已开放
```

---

## 🚨 常见问题排查

### Java 版本不匹配
```bash
# 问题: Unable to find a matching version
# 解决:
java -version
update-alternatives --config java
```

### 端口被占用
```bash
# 问题: Address already in use
# 查看占用进程:
lsof -i :8082
# 杀死进程:
kill -9 <PID>
```

### 内存不足
```bash
# 问题: Cannot allocate memory
# 检查可用内存:
free -h
# 减少 JVM 内存配置:
# -Xms1g -Xmx1g (改为 1GB)
```

### 权限问题
```bash
# 问题: Permission denied
# 解决:
sudo chown -R campus:campus /opt/campus
sudo chmod -R 755 /opt/campus
```

---

## 📝 部署确认清单

| 项目 | 完成 | 时间 | 备注 |
|------|------|------|------|
| 系统更新 | ☐ | | |
| Java 17 安装 | ☐ | | |
| Nginx 安装 | ☐ | | |
| 防火墙配置 | ☐ | | |
| JAR 部署 | ☐ | | |
| 启动脚本配置 | ☐ | | |
| Systemd 服务配置 | ☐ | | |
| 日志轮转配置 | ☐ | | |
| 监控脚本部署 | ☐ | | |
| 健康检查通过 | ☐ | | |
| API 测试通过 | ☐ | | |

---

**预计完成时间**: 2026-05-01 中午 12:00  
**下一步**: MySQL 数据库初始化 (参考 PRODUCTION_MYSQL_SETUP.md)
