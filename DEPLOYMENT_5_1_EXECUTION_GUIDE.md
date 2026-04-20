# 5/1 生产部署执行指南 - 服务器与应用部署日

**执行日期**: 2026-05-01 (周四)  
**执行时间**: 09:00 - 12:00 (3 小时)  
**状态**: 🔴 待执行 (Pending Execution)

---

## 📋 前置条件检查清单

### ✅ 环境配置确认

在开始部署前，请确保以下条件已满足：

```
生产服务器信息
☐ 服务器 IP 地址: ________________
☐ 服务器操作系统: ☐ CentOS 8.x  ☐ Ubuntu 20.04  ☐ 其他: _______
☐ SSH 用户名: ________________
☐ SSH 密钥文件路径: ________________
☐ Root 或 Sudo 权限: ☐ 已确认

网络与域名
☐ 域名: campus.edu.cn
☐ 域名 DNS 已指向服务器 IP: ☐ 已确认
☐ 防火墙已开放端口 22 (SSH): ☐ 已确认
☐ 防火墙已开放端口 80 (HTTP): ☐ 已确认
☐ 防火墙已开放端口 443 (HTTPS): ☐ 已确认
☐ 防火墙已开放端口 3306 (MySQL): ☐ 已确认
☐ 防火墙已开放端口 8082 (App): ☐ 已确认

应用文件准备
☐ JAR 文件已准备: campus-backend.jar (64.8 MB)
☐ 前端文件已构建: dist/ 目录完整
☐ 数据库初始化脚本已准备: campus_db.sql
☐ Nginx 配置文件已准备
☐ Systemd 服务文件已准备

人员与工具
☐ 运维工程师已到位
☐ DBA 已到位
☐ 开发工程师已到位
☐ QA 工程师已到位
☐ PM 已到位
☐ SSH 工具已安装 (PuTTY/OpenSSH/Xshell)
☐ curl 已安装或可用
```

---

## 🎯 5/1 执行任务分解

### 任务流程

| 阶段 | 时间 | 任务 | 负责人 | 验收标准 | 状态 |
|------|------|------|--------|---------|------|
| **阶段1** | 09:00-09:15 | 部署前准备会 | PM | 所有人员就位 | ☐ |
| **阶段2** | 09:15-09:30 | 系统环境准备 | 运维 | yum/apt 更新完成 | ☐ |
| **阶段3** | 09:30-09:40 | Java 17 安装 | 运维 | `java -version` = 17.x | ☐ |
| **阶段4** | 09:40-10:00 | Nginx 安装配置 | 运维 | Nginx 启动成功 | ☐ |
| **阶段5** | 10:00-10:20 | JAR 文件上传 | 运维 | /opt/campus/app/campus-backend.jar 存在 | ☐ |
| **阶段6** | 10:20-10:45 | Systemd 脚本配置 | 运维 | Service 文件已创建 | ☐ |
| **阶段7** | 10:45-11:05 | 应用启动验证 | 运维 | `curl localhost:8082/actuator/health` = UP | ☐ |
| **阶段8** | 11:05-11:20 | API 测试 | 开发 | 5 个 API 端点全部 200 | ☐ |
| **阶段9** | 11:20-11:35 | 日志监控配置 | 运维 | 日志轮转已部署 | ☐ |
| **阶段10** | 11:35-12:00 | 中间检查 | PM | 所有阶段 1-9 验收通过 | ☐ |

---

## 🔧 详细执行步骤 (带验证)

### 📍 09:00-09:15: 部署前准备会

**参与人员**: 运维Lead、运维工程师、DBA、开发工程师、QA工程师、PM

**议程**:
```
1. 确认所有人员已就位
2. 确认环境前置条件全部满足
3. 分配任务和职责
4. 确认通信频道和故障上报流程
5. 开始计时，记录实际开始时间
```

**验收标准**:
- [ ] 全体人员已就位
- [ ] 前置条件检查清单已填写并签署
- [ ] 沟通频道已建立 (Slack/WeChat/邮件)

**实际执行时间**: ___:___ - ___:___

---

### 📍 09:15-09:30: 系统环境准备

**负责人**: 运维工程师 1

**执行命令** (在服务器上运行):

```bash
# 1. SSH 连接到服务器
ssh -i /path/to/key ubuntu@<SERVER_IP>
# 或
ssh -u <USERNAME> <SERVER_IP>

# 2. 获取 root 权限
sudo su

# 3. 更新系统包
# For CentOS 8.x
sudo yum update -y
sudo yum install -y wget curl git vim

# For Ubuntu 20.04
sudo apt update
sudo apt upgrade -y
sudo apt install -y wget curl git vim net-tools

# 4. 禁用 SELinux (仅 CentOS)
sudo setenforce 0
sudo sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config

# 5. 创建应用目录
sudo mkdir -p /opt/campus/app
sudo mkdir -p /var/log/campus
sudo mkdir -p /etc/campus

# 6. 设置目录权限
sudo chown -R $USER:$USER /opt/campus
sudo chown -R $USER:$USER /var/log/campus
sudo chown -R $USER:$USER /etc/campus

# 7. 验证防火墙开放
# For CentOS
sudo firewall-cmd --permanent --add-port=22/tcp
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --permanent --add-port=3306/tcp
sudo firewall-cmd --permanent --add-port=8082/tcp
sudo firewall-cmd --reload

# For Ubuntu
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 3306/tcp
sudo ufw allow 8082/tcp
sudo ufw enable
```

**验证**:
```bash
# 检查防火墙状态
sudo firewall-cmd --list-all           # CentOS
sudo ufw status                         # Ubuntu

# 检查目录
ls -la /opt/campus
ls -la /var/log/campus
```

**验收标准**:
- [ ] 系统包已更新
- [ ] 必要目录已创建
- [ ] 防火墙规则已开放
- [ ] 目录权限设置正确

**实际执行时间**: ___:___ - ___:___

**记录**:
```
执行情况: □ 成功  □ 失败  □ 部分成功
问题描述: ___________________
处理结果: ___________________
```

---

### 📍 09:30-09:40: Java 17 安装

**负责人**: 运维工程师 1

**执行命令**:

```bash
# 1. 检查当前 Java 版本
java -version

# 2. For CentOS 8.x - 安装 OpenJDK 17
sudo yum install -y java-17-openjdk java-17-openjdk-devel

# For Ubuntu 20.04 - 安装 OpenJDK 17
sudo apt install -y openjdk-17-jdk openjdk-17-jre

# 3. 验证安装
java -version

# 4. 设置 JAVA_HOME (可选，通常自动)
echo $JAVA_HOME

# 5. 如需手动设置，添加到 ~/.bashrc 或 /etc/profile
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$PATH:$JAVA_HOME/bin
```

**验收标准**:
- [ ] `java -version` 返回 **openjdk 17.x.x** 或更高
- [ ] `javac -version` 返回 **javac 17.x.x**
- [ ] JAVA_HOME 环境变量已设置

**实际执行时间**: ___:___ - ___:___

**记录**:
```
Java 版本: _______________
JAVA_HOME: _______________
状态: □ 成功  □ 失败
```

---

### 📍 09:40-10:00: Nginx 安装与配置

**负责人**: 运维工程师 1

**执行命令**:

```bash
# 1. 安装 Nginx
# For CentOS 8.x
sudo yum install -y nginx

# For Ubuntu 20.04
sudo apt install -y nginx

# 2. 启动 Nginx
sudo systemctl start nginx
sudo systemctl enable nginx  # 开机自启

# 3. 验证 Nginx 运行状态
sudo systemctl status nginx

# 4. 测试 Nginx
curl http://localhost

# 5. 创建 Nginx 配置文件 (反向代理)
sudo tee /etc/nginx/conf.d/campus.conf > /dev/null <<EOF
upstream campus_backend {
    server localhost:8082;
}

server {
    listen 80;
    server_name campus.edu.cn www.campus.edu.cn;

    client_max_body_size 50M;

    location / {
        proxy_pass http://campus_backend;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_buffering off;
    }

    location /static/ {
        alias /opt/campus/web/dist/;
        expires 365d;
    }

    error_page 502 503 504 /50x.html;
    location = /50x.html {
        root /usr/share/nginx/html;
    }
}
EOF

# 6. 测试 Nginx 配置
sudo nginx -t

# 7. 重新加载 Nginx
sudo systemctl reload nginx
```

**验收标准**:
- [ ] Nginx 已安装
- [ ] Nginx 服务已启动
- [ ] `curl http://localhost` 返回 200
- [ ] 反向代理配置已创建
- [ ] `sudo nginx -t` 返回 "successful"

**实际执行时间**: ___:___ - ___:___

**记录**:
```
Nginx 版本: _______________
配置测试: □ 成功  □ 失败
状态: □ 成功  □ 失败
```

---

### 📍 10:00-10:20: JAR 文件上传

**负责人**: 运维工程师 1

**执行步骤**:

```bash
# 1. 本地文件准备 (在开发机上)
# 确保 JAR 文件已编译
ls -lh campus-backend.jar  # 应该是 64.8 MB

# 2. 上传文件到服务器
# 使用 SCP 命令
scp -i /path/to/key campus-backend.jar ubuntu@<SERVER_IP>:/tmp/

# 或使用 sftp
sftp -i /path/to/key ubuntu@<SERVER_IP>
put campus-backend.jar /tmp/

# 3. 在服务器上验证并移动文件
ssh -i /path/to/key ubuntu@<SERVER_IP>

# 验证文件大小
ls -lh /tmp/campus-backend.jar

# 复制到应用目录
sudo cp /tmp/campus-backend.jar /opt/campus/app/
sudo chown root:root /opt/campus/app/campus-backend.jar
sudo chmod 755 /opt/campus/app/campus-backend.jar

# 验证最终位置
ls -lh /opt/campus/app/campus-backend.jar
```

**验收标准**:
- [ ] JAR 文件已上传到 `/opt/campus/app/`
- [ ] 文件大小约 64.8 MB
- [ ] 文件权限正确 (755)

**实际执行时间**: ___:___ - ___:___

**文件校验**:
```
源文件大小: _____ MB
目标文件大小: _____ MB
MD5 校验 (可选): 
  源: _______________________
  目标: _______________________
状态: □ 一致  □ 不一致
```

---

### 📍 10:20-10:45: Systemd 脚本配置

**负责人**: 运维工程师 1

**执行命令**:

```bash
# 1. 创建 Systemd 服务文件
sudo tee /etc/systemd/system/campus-app.service > /dev/null <<EOF
[Unit]
Description=Campus Lost and Found System - Spring Boot Application
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/campus/app
ExecStart=/usr/bin/java -server -Xms2g -Xmx2g -XX:+UseG1GC \
  -Duser.timezone=Asia/Shanghai \
  -Dfile.encoding=UTF-8 \
  -Djava.net.preferIPv4Stack=true \
  -jar /opt/campus/app/campus-backend.jar \
  --server.port=8082 \
  --logging.file.name=/var/log/campus/app.log \
  --logging.file.max-size=10MB \
  --logging.file.max-history=7 \
  --spring.datasource.url=jdbc:mysql://localhost:3306/campus_db?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8mb4 \
  --spring.datasource.username=campus_user \
  --spring.datasource.password=Strong_Password_123!

ExecReload=/bin/kill -HUP \$MAINPID
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

# 2. 刷新 Systemd 配置
sudo systemctl daemon-reload

# 3. 启用服务自启
sudo systemctl enable campus-app.service

# 4. 验证服务配置
systemctl cat campus-app.service
```

**验收标准**:
- [ ] Systemd 服务文件已创建: `/etc/systemd/system/campus-app.service`
- [ ] 服务已启用自启
- [ ] JVM 参数正确 (-Xms2g -Xmx2g -XX:+UseG1GC)

**实际执行时间**: ___:___ - ___:___

**验证**:
```bash
systemctl status campus-app  # 应返回 inactive
systemctl cat campus-app     # 显示服务文件内容
```

---

### 📍 10:45-11:05: 应用启动与验证

**负责人**: 运维工程师 1

**执行命令**:

```bash
# 1. 启动应用
sudo systemctl start campus-app.service

# 2. 检查启动状态 (等待 5-10 秒)
sleep 5
sudo systemctl status campus-app.service

# 3. 查看应用日志
tail -50f /var/log/campus/app.log

# 4. 等待日志出现 "Started CampusApplication" 标志 (约 10-15 秒)
# 输出示例:
# 2026-05-01 10:50:15 INFO o.s.b.a.BootstrapApplicationListener - Application started with PID

# 5. 测试健康检查端点
curl http://localhost:8082/actuator/health

# 预期响应:
# {"status":"UP"}
```

**关键日志标志**:
```
✅ 启动成功标志:
  "Started CampusApplication"
  "Tomcat started on port(s): 8082"
  "Exposed health check endpoints"

❌ 启动失败标志:
  "Exception"
  "Failed to configure"
  "Cannot allocate memory"
  "Address already in use" (端口被占用)
```

**验收标准**:
- [ ] 应用进程已启动
- [ ] `curl http://localhost:8082/actuator/health` 返回 **{"status":"UP"}**
- [ ] 日志中无 ERROR 或 FATAL

**实际执行时间**: ___:___ - ___:___

**故障排查** (如启动失败):

```bash
# 检查端口占用
sudo netstat -tlnp | grep 8082
sudo lsof -i :8082

# 检查内存
free -h

# 查看完整日志
cat /var/log/campus/app.log | grep -i error

# 尝试手动启动 (用于调试)
cd /opt/campus/app
java -jar campus-backend.jar --server.port=8082
```

---

### 📍 11:05-11:20: API 端点测试

**负责人**: 开发工程师 1

**测试 5 个关键 API 端点**:

```bash
# 1. 健康检查
curl -X GET http://localhost:8082/actuator/health
# 预期: {"status":"UP"}

# 2. 首页数据
curl -X GET http://localhost:8082/api/posts/search?page=0
# 预期: HTTP 200, 返回数据

# 3. 分类列表
curl -X GET http://localhost:8082/api/categories
# 预期: HTTP 200, 返回分类数据

# 4. 用户登录 (如适用)
curl -X POST http://localhost:8082/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
# 预期: HTTP 200, 返回 token

# 5. 发布帖子 (如适用)
curl -X POST http://localhost:8082/api/posts \
  -H "Content-Type: application/json" \
  -d '{"title":"测试帖子","description":"测试内容"}'
# 预期: HTTP 200/201, 返回新帖子 ID
```

**测试脚本** (保存为 `test-api-5-1.sh`):

```bash
#!/bin/bash

echo "=== API 端点测试 ==="
echo "时间: $(date)"
echo ""

PASS=0
FAIL=0

# 测试 1: 健康检查
echo "测试 1: 健康检查"
RESULT=$(curl -s http://localhost:8082/actuator/health)
if echo "$RESULT" | grep -q "UP"; then
    echo "✅ 通过"
    ((PASS++))
else
    echo "❌ 失败: $RESULT"
    ((FAIL++))
fi
echo ""

# 测试 2-5 类似...

echo "========= 测试总结 ========="
echo "通过: $PASS"
echo "失败: $FAIL"
echo "总计: $((PASS + FAIL))"

if [ $FAIL -eq 0 ]; then
    echo "✅ 所有测试通过"
    exit 0
else
    echo "❌ 存在失败项"
    exit 1
fi
```

**验收标准**:
- [ ] 所有 5 个 API 端点均返回 HTTP 2xx
- [ ] 响应数据格式正确
- [ ] 无超时或连接错误

**实际执行时间**: ___:___ - ___:___

**测试结果记录**:

| 端点 | 方法 | 状态码 | 响应 | 状态 |
|------|------|--------|------|------|
| /actuator/health | GET | | | ☐ |
| /api/posts/search | GET | | | ☐ |
| /api/categories | GET | | | ☐ |
| /api/users/login | POST | | | ☐ |
| /api/posts | POST | | | ☐ |

---

### 📍 11:20-11:35: 日志与监控配置

**负责人**: 运维工程师 1

**执行命令**:

```bash
# 1. 配置日志轮转 (/etc/logrotate.d/campus)
sudo tee /etc/logrotate.d/campus > /dev/null <<EOF
/var/log/campus/*.log {
    daily
    rotate 7
    compress
    delaycompress
    notifempty
    create 0640 root root
    sharedscripts
    postrotate
        systemctl reload campus-app > /dev/null 2>&1 || true
    endscript
}
EOF

# 2. 创建监控脚本 (/usr/local/bin/campus_monitor.sh)
sudo tee /usr/local/bin/campus_monitor.sh > /dev/null <<'EOF'
#!/bin/bash

# 应用监控脚本 - 每 60 秒检查一次

while true; do
    TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
    
    # 检查应用是否运行
    if curl -s http://localhost:8082/actuator/health | grep -q "UP"; then
        echo "[$TIMESTAMP] ✅ 应用运行正常"
    else
        echo "[$TIMESTAMP] ❌ 应用异常，尝试重启..."
        sudo systemctl restart campus-app
    fi
    
    # 检查磁盘空间
    DISK=$(df / | awk 'NR==2 {print $5}' | sed 's/%//')
    if [ $DISK -gt 80 ]; then
        echo "[$TIMESTAMP] ⚠️  磁盘使用率: ${DISK}%"
    fi
    
    sleep 60
done
EOF

sudo chmod +x /usr/local/bin/campus_monitor.sh

# 3. 创建 systemd 监控服务 (可选)
sudo tee /etc/systemd/system/campus-monitor.service > /dev/null <<EOF
[Unit]
Description=Campus Application Monitor
After=campus-app.service

[Service]
Type=simple
ExecStart=/usr/local/bin/campus_monitor.sh
Restart=always
StandardOutput=journal

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable campus-monitor.service
```

**验收标准**:
- [ ] 日志轮转配置已创建
- [ ] 监控脚本已创建并可执行
- [ ] 监控脚本已测试

**实际执行时间**: ___:___ - ___:___

---

### 📍 11:35-12:00: 中间检查与同步

**负责人**: PM

**检查清单**:

```
✅ 第 1 部分完成确认:

系统环境:
  □ 防火墙规则已开放
  □ 目录已创建并授权

Java 环境:
  □ OpenJDK 17 已安装
  □ JAVA_HOME 已设置

Nginx:
  □ Nginx 已安装并运行
  □ 反向代理配置已生效

应用部署:
  □ JAR 文件已上传
  □ Systemd 服务已配置
  □ 应用已启动

验证:
  □ /actuator/health 返回 UP
  □ 5 个 API 端点全部 200
  □ 日志已配置

监控:
  □ 日志轮转已配置
  □ 监控脚本已部署
```

**同步会议** (12:00):
- 回顾 5/1 第 1 部分执行情况
- 确认是否有问题需要解决
- 宣布 5/2 数据库部署日程
- 确认 5/2 参与人员已准备

**预期结果**: 如无问题，第 2 部分（5/2 数据库部署）可按计划进行

---

## 📊 执行状态总结

**5/1 部署日执行状态**:

| 任务 | 预计时间 | 实际时间 | 状态 | 备注 |
|------|---------|---------|------|------|
| 部署前准备 | 09:00-09:15 | |  |  |
| 系统环境准备 | 09:15-09:30 | |  |  |
| Java 17 安装 | 09:30-09:40 | |  |  |
| Nginx 安装 | 09:40-10:00 | |  |  |
| JAR 上传 | 10:00-10:20 | |  |  |
| Systemd 配置 | 10:20-10:45 | |  |  |
| 应用启动 | 10:45-11:05 | |  |  |
| API 测试 | 11:05-11:20 | |  |  |
| 日志监控 | 11:20-11:35 | |  |  |
| 中间检查 | 11:35-12:00 | |  |  |

**总耗时**: 预计 3 小时（09:00-12:00）

---

## 🔐 故障排查速查表

### 问题: Java 版本错误
```bash
# 症状: java -version 显示 Java 8 或 11
# 解决:
sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-17-openjdk*/bin/java 1
java -version
```

### 问题: 端口已被占用
```bash
# 症状: "Address already in use: 8082"
# 解决:
sudo lsof -i :8082
sudo kill -9 <PID>
```

### 问题: 应用启动缓慢或无法启动
```bash
# 症状: curl 返回 "Connection refused"
# 解决:
# 1. 检查内存是否充足
free -h
# 2. 检查磁盘空间
df -h
# 3. 查看详细日志
tail -100 /var/log/campus/app.log | grep -i error
```

### 问题: Nginx 反向代理无法连接后端
```bash
# 症状: nginx 返回 502 Bad Gateway
# 解决:
# 1. 检查后端应用是否运行
curl http://localhost:8082/actuator/health
# 2. 重新加载 nginx
sudo nginx -t
sudo systemctl reload nginx
```

---

## ✍️ 执行人员签署

**开始时间**: 09:00  
**完成时间**: ___:___

| 角色 | 姓名 | 签名 | 时间 |
|------|------|------|------|
| 运维 Lead | | | |
| 运维工程师 | | | |
| 开发工程师 | | | |
| PM | | | |

**总体状态**: □ ✅ 成功  □ ⚠️ 部分成功  □ ❌ 失败

**问题记录**:
```
问题 1: 
  描述: ___________________________
  影响: ___________________________
  解决: ___________________________

问题 2:
  描述: ___________________________
  影响: ___________________________
  解决: ___________________________
```

**下阶段准备**:
- [ ] 5/2 数据库部署日所需文件已准备
- [ ] DBA 已确认 5/2 时间可用
- [ ] 数据库初始化脚本已验证

---

## 📌 关键联系与资源

**部署阶段文档**:
- 📄 [PRODUCTION_SERVER_SETUP.md](PRODUCTION_SERVER_SETUP.md) - 详细参考
- 📄 [PRODUCTION_MYSQL_SETUP.md](PRODUCTION_MYSQL_SETUP.md) - 5/2 用
- 📄 [PRODUCTION_HTTPS_SETUP.md](PRODUCTION_HTTPS_SETUP.md) - 5/3 用
- 📄 [PRODUCTION_DEPLOYMENT_SUMMARY.md](PRODUCTION_DEPLOYMENT_SUMMARY.md) - 总体计划

**应急联系**:
```
运维 Lead:        ________________
DBA:              ________________
开发主力:         ________________
PM:               ________________
技术支持:         ________________
```

**关键链接**:
```
生产服务器 IP:    ________________
域名:             campus.edu.cn
管理后台:         http://campus.edu.cn/admin (5/1后)
监控平台:         ________________
日志系统:         ________________
```

---

**最后更新**: 2026-04-21  
**执行版本**: v1.0.0 Final  
**状态**: 🟢 准备就绪
