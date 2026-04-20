# 校园失物招领系统 - HTTPS 证书配置指南

**版本**: v1.0.0  
**生效时间**: 2026-05-02 (在 PRODUCTION_MYSQL_SETUP.md 之后)  
**目标环境**: 生产 Nginx + Let's Encrypt SSL  
**预计时间**: 30-45 分钟

---

## 📋 前置条件

### 确认已完成
- [x] 生产服务器已部署 (见 PRODUCTION_SERVER_SETUP.md)
- [x] MySQL 数据库已初始化 (见 PRODUCTION_MYSQL_SETUP.md)
- [x] Nginx 已安装并配置反向代理
- [x] 域名已在 DNS 中指向服务器 IP

### 需要的信息
```
✅ 域名: campus.edu.cn (或您的实际域名)
✅ 服务器 IP: 1.2.3.4 (示例)
✅ DNS 已生效: ping campus.edu.cn 应该返回服务器 IP
✅ 防火墙已开放: 80 端口 (用于 Let's Encrypt 验证)
```

---

## 🔐 第 1 步: 获取 SSL 证书 (10 分钟)

### 方案 A: 使用 Let's Encrypt (免费，推荐)

#### 1.1 安装 Certbot

**CentOS 8.x**:
```bash
sudo yum install -y certbot python3-certbot-nginx
```

**Ubuntu 20.04**:
```bash
sudo apt install -y certbot python3-certbot-nginx
```

#### 1.2 申请证书 (方案 1: 自动配置，简单)

```bash
# 停止 Nginx (让 Certbot 使用 80 端口)
sudo systemctl stop nginx

# 申请证书 (会自动配置 Nginx)
sudo certbot certonly --standalone \
    --email admin@campus.edu.cn \
    --agree-tos \
    -d campus.edu.cn \
    -d www.campus.edu.cn

# 交互式提示:
# Saving debug log to /var/log/letsencrypt/letsencrypt.log
# Plugins selected: Authenticator (standalone), Installer (nginx)
# Account registered.
# Requesting a certificate for campus.edu.cn and www.campus.edu.cn
# 
# IMPORTANT NOTES:
#  - Congratulations! Your certificate and chain have been saved at:
#    /etc/letsencrypt/live/campus.edu.cn/fullchain.pem
#    Your key file has been saved at:
#    /etc/letsencrypt/live/campus.edu.cn/privkey.pem
```

#### 1.3 申请证书 (方案 2: 手动配置，更灵活)

```bash
# 只申请证书，不自动配置 Nginx
sudo certbot certonly --preferred-challenges=http \
    -d campus.edu.cn \
    -d www.campus.edu.cn \
    --email admin@campus.edu.cn \
    --agree-tos \
    --non-interactive

# 证书会保存到:
# /etc/letsencrypt/live/campus.edu.cn/
```

### 方案 B: 使用付费证书 (可选，适合企业)

如果您已有付费 SSL 证书，按照您的证书提供商的说明部署:

```bash
# 将证书文件上传到服务器
# 通常包括:
# - certificate.crt (公钥证书)
# - private.key (私钥)
# - ca_bundle.crt (中间证书，可选)

scp certificate.crt root@your-server:/tmp/
scp private.key root@your-server:/tmp/
scp ca_bundle.crt root@your-server:/tmp/

# 复制到 Nginx 目录
sudo mkdir -p /etc/nginx/ssl
sudo cp /tmp/certificate.crt /etc/nginx/ssl/campus.edu.cn.crt
sudo cp /tmp/private.key /etc/nginx/ssl/campus.edu.cn.key
sudo cp /tmp/ca_bundle.crt /etc/nginx/ssl/ca_bundle.crt

# 设置权限
sudo chmod 600 /etc/nginx/ssl/campus.edu.cn.key
sudo chmod 644 /etc/nginx/ssl/campus.edu.cn.crt
```

---

## 🔧 第 2 步: 配置 Nginx SSL (15 分钟)

### 2.1 创建 Nginx SSL 配置

**更新 Nginx 配置** (`/etc/nginx/conf.d/campus.conf`):
```nginx
# ============ HTTP 配置 ============
server {
    listen 80;
    listen [::]:80;
    server_name campus.edu.cn www.campus.edu.cn;

    # Let's Encrypt 验证路径 (续期时需要)
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    # 其他请求重定向到 HTTPS
    location / {
        return 301 https://$server_name$request_uri;
    }
}

# ============ HTTPS 配置 ============
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name campus.edu.cn www.campus.edu.cn;

    # SSL 证书配置 (Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/campus.edu.cn/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/campus.edu.cn/privkey.pem;

    # 或付费证书
    # ssl_certificate /etc/nginx/ssl/campus.edu.cn.crt;
    # ssl_certificate_key /etc/nginx/ssl/campus.edu.cn.key;

    # SSL 参数优化
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    # HSTS (强制 HTTPS, 1 年)
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    
    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # 日志文件
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
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Swagger 文档
    location /doc.html {
        proxy_pass http://localhost:8082;
        proxy_set_header Host $host;
    }

    # 健康检查 (不记录日志)
    location = /health {
        proxy_pass http://localhost:8082/actuator/health;
        access_log off;
    }
}
```

### 2.2 验证 Nginx 配置

```bash
# 检查配置语法
sudo nginx -t
# 输出应该是:
# nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
# nginx: configuration file /etc/nginx/nginx.conf test is successful
```

### 2.3 重启 Nginx

```bash
# 重新加载配置 (不中断现有连接)
sudo systemctl reload nginx

# 或完全重启 (推荐)
sudo systemctl restart nginx

# 验证状态
sudo systemctl status nginx
```

---

## ✅ 第 3 步: 验证 HTTPS 配置 (10 分钟)

### 3.1 浏览器验证

```
1. 打开浏览器访问: https://campus.edu.cn
2. 查看地址栏左边，应该看到:
   ✅ 绿色锁图标 (表示 HTTPS 有效)
   ✅ 证书信息 (点击可查看证书详情)
   ✅ URL 显示 https://campus.edu.cn
3. 检查页面是否正常加载
   ✅ 前端页面显示
   ✅ API 调用正常 (查看浏览器开发工具 Network 标签)
```

### 3.2 命令行验证

```bash
# 测试 HTTP 到 HTTPS 重定向
curl -I http://campus.edu.cn
# 应该看到:
# HTTP/1.1 301 Moved Permanently
# Location: https://campus.edu.cn/

# 测试 HTTPS 连接
curl -I https://campus.edu.cn
# 应该看到:
# HTTP/2 200
# server: nginx

# 查看证书信息
openssl s_client -connect campus.edu.cn:443 -servername campus.edu.cn
# 按 Ctrl+C 退出
# 应该看到证书链和有效期

# 验证证书有效期
curl -s https://campus.edu.cn 2>&1 | openssl x509 -noout -dates
# 应该看到:
# notBefore=Apr 19 00:00:00 2026 GMT
# notAfter=Jul 18 23:59:59 2026 GMT

# 使用 OpenSSL 查看证书详情
openssl x509 -in /etc/letsencrypt/live/campus.edu.cn/fullchain.pem -noout -text
```

### 3.3 在线检测工具

使用以下免费工具检查 SSL 配置:
- [SSL Labs SSL Test](https://www.ssllabs.com/ssltest/analyze.html?d=campus.edu.cn) - 详细的 SSL/TLS 配置评分
- [Mozilla Observatory](https://observatory.mozilla.org/analyze/campus.edu.cn) - 安全头检测
- [Qualys SSL Client Test](https://www.ssllabs.com/ssltest/viewMyClient.html) - 客户端兼容性

---

## 🔄 第 4 步: 证书自动续期 (5 分钟)

### 4.1 配置自动续期 (Let's Encrypt)

```bash
# Let's Encrypt 证书有效期: 90 天
# 需要定期续期

# 测试续期 (不会真正续期，只是检查)
sudo certbot renew --dry-run

# 查看续期计划任务
sudo systemctl list-timers | grep certbot

# 如果没有自动任务，手动创建
sudo crontab -e
# 添加:
# 0 3 * * * /usr/bin/certbot renew --quiet && /usr/sbin/systemctl reload nginx
```

### 4.2 配置证书过期提醒

```bash
# 编辑 certbot 配置以发送提醒
sudo vi /etc/letsencrypt/renewal/campus.edu.cn.conf

# 添加邮件通知 (可选)
# email = admin@campus.edu.cn
# authenticator = standalone
```

---

## 🔒 第 5 步: 安全性加固 (10 分钟)

### 5.1 配置安全头

**更新 Nginx 配置中已包含的安全头**:
```nginx
# 防止点击劫持
add_header X-Frame-Options "SAMEORIGIN" always;

# 防止 MIME 类型嗅探
add_header X-Content-Type-Options "nosniff" always;

# 启用 XSS 防护
add_header X-XSS-Protection "1; mode=block" always;

# 控制 Referrer 信息
add_header Referrer-Policy "strict-origin-when-cross-origin" always;

# 强制 HTTPS (HSTS)
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

# 内容安全策略 (CSP) - 可选，根据需要调整
# add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'" always;
```

### 5.2 禁用不安全的 TLS 版本

```nginx
# 只允许 TLS 1.2 及以上 (已在配置中)
ssl_protocols TLSv1.2 TLSv1.3;

# 禁用弱密码
ssl_ciphers HIGH:!aNULL:!MD5:!3DES;
```

### 5.3 配置日志和监控

```bash
# 查看 Nginx 访问日志
tail -f /var/log/nginx/campus_access.log

# 查看 Nginx 错误日志
tail -f /var/log/nginx/campus_error.log

# 监控 SSL 连接
sudo tcpdump -i eth0 'port 443' -n

# 查看当前 HTTPS 连接
netstat -tuln | grep 443
```

---

## 📊 第 6 步: 性能优化 (10 分钟)

### 6.1 启用 HTTP/2

```nginx
# 在 server 块中已配置
listen 443 ssl http2;

# 验证 HTTP/2 是否启用
curl -I --http2 https://campus.edu.cn
# 应该看到 HTTP/2 200
```

### 6.2 配置压缩

**在 Nginx 配置中添加**:
```nginx
# 启用 gzip 压缩
gzip on;
gzip_min_length 1000;
gzip_proxied any;
gzip_types text/plain text/css text/js text/xml text/javascript 
           application/x-javascript application/xml+rss application/json;
gzip_vary on;
gzip_comp_level 6;
```

### 6.3 配置缓存

```nginx
# 对静态文件配置长期缓存 (已配置)
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
    expires 365d;
    add_header Cache-Control "public, immutable";
}

# 对 HTML 文件配置短期缓存
location ~* \.html$ {
    expires 1d;
    add_header Cache-Control "public";
}
```

---

## 🧪 第 7 步: 完整测试 (15 分钟)

### 7.1 端到端功能测试

```bash
# 1. 测试首页访问
curl -I https://campus.edu.cn
# HTTP/2 200 ✅

# 2. 测试 API - 获取物品列表
curl https://campus.edu.cn/api/items | jq .
# 应该返回 JSON 数组

# 3. 测试 API - 搜索功能
curl "https://campus.edu.cn/api/search?keyword=iPhone" | jq .
# 应该返回搜索结果

# 4. 测试 API - 健康检查
curl https://campus.edu.cn/health
# {"status":"UP"}

# 5. 测试健康检查
curl https://campus.edu.cn/actuator/health
# {"status":"UP"}
```

### 7.2 性能测试

```bash
# 测试 HTTPS 响应时间
curl -w "时间: %{time_total}s\n" https://campus.edu.cn

# 使用 ab (Apache Bench) 进行负载测试
ab -n 100 -c 10 -k https://campus.edu.cn/

# 使用 wrk 进行更复杂的性能测试
wrk -t4 -c100 -d30s https://campus.edu.cn/
```

### 7.3 安全测试

```bash
# 测试 HSTS 头
curl -I https://campus.edu.cn | grep Strict-Transport-Security
# Strict-Transport-Security: max-age=31536000; includeSubDomains

# 测试 X-Frame-Options
curl -I https://campus.edu.cn | grep X-Frame-Options
# X-Frame-Options: SAMEORIGIN

# 测试 SSL/TLS 配置强度
echo | openssl s_client -connect campus.edu.cn:443 -servername campus.edu.cn 2>/dev/null | grep -E "Protocol|Cipher"
# Protocol: TLSv1.3
# Cipher: TLS_AES_256_GCM_SHA384
```

---

## ✅ HTTPS 部署验证清单

| 检查项 | 命令/工具 | 预期结果 | 完成 |
|--------|---------|---------|------|
| 证书获取 | `ls /etc/letsencrypt/live/campus.edu.cn/` | fullchain.pem, privkey.pem 存在 | ☐ |
| Nginx 配置 | `sudo nginx -t` | syntax is ok | ☐ |
| HTTP 重定向 | `curl -I http://campus.edu.cn` | HTTP/1.1 301 | ☐ |
| HTTPS 连接 | `curl -I https://campus.edu.cn` | HTTP/2 200 | ☐ |
| 证书有效 | 浏览器访问 | 绿色锁图标 | ☐ |
| 证书链 | `openssl s_client` | 完整的证书链 | ☐ |
| 安全头 | `curl -I https://campus.edu.cn` | 包含 HSTS, X-Frame-Options 等 | ☐ |
| HTTP/2 | `curl --http2` | HTTP/2 200 | ☐ |
| 性能 | `curl -w "时间: %{time_total}s\n"` | < 1 秒 | ☐ |
| 自动续期 | `sudo systemctl list-timers certbot` | 计划任务存在 | ☐ |

---

## 🚨 常见问题排查

### 证书获取失败
```bash
# 问题: Certbot error getting validation data
# 原因: 80 端口不可访问或 DNS 未生效
# 解决:
# 1. 确保防火墙开放 80 端口: sudo firewall-cmd --add-port=80/tcp
# 2. 验证 DNS: dig campus.edu.cn (应该返回正确的 IP)
# 3. 验证连接: curl http://your-ip:80
```

### Nginx 配置错误
```bash
# 问题: nginx: [error] invalid number of arguments
# 解决: 检查配置文件中是否有语法错误
sudo nginx -t -c /etc/nginx/nginx.conf
# 查看具体错误信息
```

### 证书过期
```bash
# 问题: SSL_ERROR_BAD_CERT_DOMAIN 或证书已过期
# 解决:
# 手动续期
sudo certbot renew --force-renewal

# 检查证书有效期
openssl x509 -in /etc/letsencrypt/live/campus.edu.cn/fullchain.pem -noout -dates

# 重启 Nginx
sudo systemctl reload nginx
```

### 混合内容错误
```bash
# 问题: Mixed Content (http 和 https 混合)
# 原因: 页面通过 HTTPS 加载，但某些资源使用 HTTP
# 解决: 在 Nginx 配置中更新资源 URL 为相对路径或 HTTPS
# 或在 HTML head 中添加:
# <meta http-equiv="Content-Security-Policy" content="upgrade-insecure-requests">
```

---

## 📅 证书维护计划

| 日期 | 任务 | 负责人 | 状态 |
|------|------|--------|------|
| 5/2 | HTTPS 初始配置 | 运维 | ☐ |
| 5/2 | 测试验证 | QA | ☐ |
| 5/7 | 生产环境启用 | 运维 | ☐ |
| 6/30 | 证书续期检查 | 运维 | ☐ |
| 7/17 | 证书续期 (90天期限) | 自动 | - |
| 每月 | SSL 配置审计 | 运维 | ☐ |

---

## 📋 最后检查清单 (上线前)

### 在 5/7 上线前确保:
- [ ] HTTPS 证书已申请并配置
- [ ] Nginx 已配置 SSL 和反向代理
- [ ] HTTP 已配置自动重定向到 HTTPS
- [ ] 浏览器访问显示绿色锁图标
- [ ] API 通过 HTTPS 可正常访问
- [ ] 安全头已配置 (HSTS, CSP 等)
- [ ] 自动续期任务已配置
- [ ] SSL 配置强度已验证 (A+ 评分)
- [ ] 性能测试已通过 (< 1s 响应)
- [ ] 备用证书已保存

---

**预计完成时间**: 2026-05-02 下午 16:00  
**后续任务**: Smoke Test & 性能基线采集 (5/3)
