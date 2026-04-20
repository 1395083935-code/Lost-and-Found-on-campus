# 校园失物招领系统 - 团队交接文档 (Team Handoff)

**编制日期**: 2026-04-21  
**交接内容**: Campus Lost & Found v1.0.0 系统代码、文档、部署流程  
**交接对象**: 运维团队、后续维护团队  
**有效期**: 正式上线至系统退役

---

## 📌 快速导航

- **问题反馈**: 发送邮件至 tech-support@campus.edu.cn
- **紧急热线**: 技术负责人 [电话待确认]
- **工作时间**: 周一-周五 9:00-18:00，周六应急 (提前 2 小时通知)
- **文档位置**: d:\桌面\workspace 及 GitHub 代码仓库

---

## 👥 团队结构与联系方式

### 开发团队
| 角色 | 姓名 | 电话 | 邮件 | 专长 |
|------|------|------|------|------|
| 后端负责人 | [待确认] | [待确认] | [待确认] | Spring Boot, MySQL |
| 前端负责人 | [待确认] | [待确认] | [待确认] | Vue.js, 页面设计 |
| 全栈工程师 | [待确认] | [待确认] | [待确认] | API 设计, 性能优化 |

### QA 团队
| 角色 | 姓名 | 电话 | 邮件 |
|------|------|------|------|
| 测试负责人 | AI QA Agent | N/A | ai-qa@system.local |
| 自动化测试工程师 | [待确认] | [待确认] | [待确认] |

### 运维团队 (交接接收方)
| 角色 | 姓名 | 电话 | 邮件 |
|------|------|------|------|
| 运维经理 | [待确认] | [待确认] | [待确认] |
| 数据库管理员 | [待确认] | [待确认] | [待确认] |
| 系统管理员 | [待确认] | [待确认] | [待确认] |

---

## 📂 代码仓库结构

### 仓库位置
```
Campus-Lost-Found-System/
├── campus-backend/                 # 后端 Spring Boot 项目
│   ├── pom.xml                     # Maven 依赖配置
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/               # 源代码
│   │   │   │   ├── controller/     # API 接口
│   │   │   │   ├── service/        # 业务逻辑
│   │   │   │   ├── mapper/         # 数据库 DAO
│   │   │   │   ├── entity/         # 数据模型
│   │   │   │   └── config/         # 配置类
│   │   │   └── resources/
│   │   │       ├── application.yml # 应用配置
│   │   │       ├── application-dev.yml
│   │   │       ├── application-prod.yml
│   │   │       └── db/             # 数据库脚本
│   │   └── test/                   # 单元测试
│   └── target/                     # 构建输出 (JAR 文件)
│
├── campus-frontend/                # 前端 Vue.js 项目
│   ├── package.json                # npm 依赖配置
│   ├── src/
│   │   ├── components/             # Vue 组件
│   │   ├── pages/                  # 页面
│   │   ├── router/                 # 路由配置
│   │   ├── styles/                 # CSS 样式
│   │   └── App.vue                 # 根组件
│   ├── public/                     # 静态资源
│   └── dist/                       # 构建输出 (生产包)
│
├── PRDs/                           # 产品需求文档
│   ├── PRD.md                      # 功能需求
│   ├── development.md              # 开发规范
│   └── UI.md                       # 设计规范
│
└── docs/                           # 上线相关文档
    ├── LAUNCH_GUIDE.md             # 上线指南
    ├── OPERATIONS_MANUAL.md        # 运维手册
    ├── INCIDENT_RESPONSE_PLAN.md   # 应急预案
    ├── TEAM_HANDOFF.md             # 团队交接
    ├── PERFORMANCE_TEST_REPORT.md  # 性能报告
    └── REGRESSION_TEST_REPORT.md   # 回归测试报告
```

### 版本标记
- **Latest Stable**: v1.0.0 (2026-04-20 发布)
- **Git Tag**: git checkout v1.0.0
- **JAR 文件**: campus-backend-0.0.1-SNAPSHOT.jar (64.8 MB)

---

## 🔨 关键文件与配置

### 后端关键配置文件

#### application.yml (生产环境)
```yaml
server:
  port: 8082
spring:
  datasource:
    url: jdbc:mysql://[DB_HOST]:3306/campus_db?useUnicode=true&characterEncoding=utf-8
    username: ${DB_USER}
    password: ${DB_PASS}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: none
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never
```

#### pom.xml 关键依赖
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.1.10</version>
</dependency>
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.8</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 前端关键配置

#### package.json 脚本
```json
{
  "scripts": {
    "serve": "vue-cli-service serve",
    "build": "vue-cli-service build",
    "lint": "vue-cli-service lint"
  }
}
```

#### 环境变量 (.env.production)
```
VUE_APP_API_BASE_URL=http://campus-api.your-domain.com:8082
VUE_APP_ENV=production
```

---

## 📚 关键文档清单

### 必读文档 (上线前必须阅读)
1. ✅ [PRE_LAUNCH_CHECKLIST.md](PRE_LAUNCH_CHECKLIST.md) - 上线前最终检查清单
2. ✅ [LAUNCH_GUIDE.md](LAUNCH_GUIDE.md) - 上线指南与部署步骤
3. ✅ [OPERATIONS_MANUAL.md](OPERATIONS_MANUAL.md) - 日常运维手册
4. ✅ [INCIDENT_RESPONSE_PLAN.md](INCIDENT_RESPONSE_PLAN.md) - 应急响应计划

### 参考文档
5. ✅ [PRD.md](PRD.md) - 产品需求 (功能理解)
6. ✅ [PERFORMANCE_TEST_REPORT.md](PERFORMANCE_TEST_REPORT.md) - 性能基线
7. ✅ [REGRESSION_TEST_REPORT.md](REGRESSION_TEST_REPORT.md) - 测试覆盖

### 技术文档
8. ✅ [development.md](development.md) - 开发规范与技术栈
9. ✅ [API Documentation](http://localhost:8082/doc.html) - Swagger/Knife4j 在线 API 文档

---

## 🚀 部署与启动

### 快速启动 (开发/测试环境)
```bash
# 后端启动
cd campus-backend
mvn -DskipTests clean package
java -jar target/campus-backend-0.0.1-SNAPSHOT.jar --server.port=8082

# 前端启动
cd campus-frontend
npm install
npm run serve  # 开发服务器 (http://localhost:8081)
npm run build  # 生产构建
```

### 生产环境部署 (详见 LAUNCH_GUIDE.md)
```bash
# JAR 后台运行
nohup java -jar campus-backend-0.0.1-SNAPSHOT.jar > logs/app.log 2>&1 &

# 前端部署到 Nginx
# 1. 构建: npm run build
# 2. 复制 dist 文件到 /usr/share/nginx/html/
# 3. 配置 Nginx 反向代理至后端
```

---

## 📋 日常维护手册

### 常见操作

#### 1. 查看后端日志
```bash
# 实时日志
tail -f logs/app.log

# 查看最后 100 行
tail -100 logs/app.log

# 搜索错误
grep "ERROR" logs/app.log | tail -20
```

#### 2. 重启后端服务
```bash
# 杀死现有进程
pkill -f "campus-backend"

# 重新启动
nohup java -jar campus-backend-0.0.1-SNAPSHOT.jar > logs/app.log 2>&1 &
```

#### 3. 数据库备份
```bash
# 全库备份
mysqldump -u root -p campus_db > backup_$(date +%Y%m%d_%H%M%S).sql

# 恢复备份
mysql -u root -p campus_db < backup_20260407_150000.sql
```

#### 4. 查看应用状态
```bash
# 健康检查
curl http://localhost:8082/actuator/health

# 应用信息
curl http://localhost:8082/actuator/info
```

### 监控与告警

#### 关键指标监控
- **CPU 使用率**: > 80% 报警
- **内存使用率**: > 85% 报警
- **磁盘使用率**: > 90% 报警
- **API 错误率**: > 1% 报警
- **API 响应时间**: P95 > 500ms 报警

#### 日志告警关键字
```
ERROR     - 应用错误
EXCEPTION - 异常堆栈
TIMEOUT   - 超时
CONNECTION_REFUSED - 连接拒绝
```

---

## 🔧 故障排查指南

### 常见问题与解决方案

#### 问题 1: 后端无法启动
**症状**: 启动 JAR 后立即退出或卡住  
**原因**: 端口被占用、数据库连接失败、配置错误  
**解决方案**:
```bash
# 检查端口占用
netstat -tulpn | grep 8082

# 检查数据库连接
mysql -h 127.0.0.1 -u root -p -e "SELECT 1;"

# 查看启动日志
cat logs/app.log | grep "ERROR\|ERROR\|WARN"
```

#### 问题 2: 前端页面白屏
**症状**: 打开首页显示空白，控制台有 JavaScript 错误  
**原因**: 后端 API 不可用、CORS 配置错误、静态资源加载失败  
**解决方案**:
```bash
# 检查后端服务
curl http://localhost:8082/api/items

# 检查前端打包
npm run build && npm run serve

# 清理浏览器缓存 (Ctrl+Shift+Delete)
```

#### 问题 3: 数据库查询慢
**症状**: 列表加载缓慢，搜索响应时间 > 500ms  
**原因**: 缺少索引、查询条件不当、表数据过多  
**解决方案**:
```sql
-- 检查索引
SHOW INDEX FROM lost_found_item;

-- 分析查询性能
EXPLAIN SELECT * FROM lost_found_item WHERE status = 1 ORDER BY create_time DESC;

-- 创建缺失的索引
CREATE INDEX idx_status_create_time ON lost_found_item(status, create_time);
```

#### 问题 4: 用户反馈无法登录
**症状**: 用户微信授权后跳转回来仍显示未登录  
**原因**: Token 过期、微信 openid 获取失败、 localStorage 清空  
**解决方案**:
```javascript
// 前端排查: 检查 localStorage 中的 token
localStorage.getItem('token')

// 后端排查: 查看用户表中是否有该用户
SELECT * FROM user WHERE openid = 'xxxxx';
```

---

## 🔐 安全与合规

### 数据安全
- ✅ 用户密码 BCrypt 加密存储
- ✅ 敏感数据 (手机号) 脱敏显示
- ✅ 所有 API 使用 JWT Token 认证
- ✅ 数据库连接配置加密存储

### 定期维护任务
- **每周**: 检查错误日志，查看异常趋势
- **每月**: 数据库备份验证，系统性能评估
- **每季**: 依赖库安全更新，代码审查优化
- **每年**: 灾难恢复演练，架构升级评估

---

## 📞 技术支持与升级

### 当前支持周期
- **开发支持**: 上线后 3 个月 (2026-05-07 至 2026-08-07)
- **缺陷修复**: 优先级 P0/P1 24h 内修复，P2/P3 周内修复
- **功能迭代**: 按需求优先级，2 周一版本

### 后续功能规划 (可选)
- [ ] 智能匹配提醒 (基于关键词自动配对)
- [ ] 校园地图定位 (丢失地点地图标记)
- [ ] 诚信积分体系 (用户行为评分)
- [ ] 消息推送升级 (微信服务号通知)

---

## ✅ 交接确认清单

### 开发团队交接清单
- [x] 所有源代码已上传至 Git 仓库，标记 v1.0.0
- [x] 数据库脚本 (campus_db.sql) 已提供
- [x] 环境配置文件 (application-prod.yml) 已准备
- [x] 依赖管理 (pom.xml, package.json) 已锁定版本
- [x] 部署脚本已编写，可一键启动
- [x] API 文档已生成，Swagger 接口可用

### QA 团队交接清单
- [x] 回归测试通过率 100% (10/10 用例)
- [x] 性能测试 P95 达标 (4/4 路径)
- [x] 安全审计完成，5/5 安全检查通过
- [x] 缺陷追踪: P1 已修复，P0 为 0
- [x] 测试脚本已交接，可重复执行
- [x] 测试报告已生成，数据完整

### 运维团队交接清单 (待确认)
- [ ] 服务器环境已准备 (2C4G+ 配置)
- [ ] MySQL 数据库已安装并初始化
- [ ] HTTPS 证书已申请并配置
- [ ] 监控告警系统已部署
- [ ] 备份策略已落实 (每天 2 次)
- [ ] 故障恢复演练已完成

---

## 📝 交接签署

**开发团队代表**: ________________  日期: _________

**QA 团队代表**: ________________  日期: _________

**运维团队代表**: ________________  日期: _________

**项目经理**: ________________  日期: _________

---

## 📚 附录: 常用命令速查表

```bash
# 后端相关
mvn clean package                          # 构建 JAR
java -jar app.jar                          # 启动应用
curl http://localhost:8082/actuator/health # 健康检查

# 前端相关
npm install                                # 安装依赖
npm run serve                              # 开发环境
npm run build                              # 生产构建

# 数据库相关
mysql -u root -p campus_db                 # 连接数据库
SHOW TABLES;                               # 查看表列表
SHOW INDEX FROM table_name;                # 查看索引

# 系统相关
ps aux | grep java                         # 查看进程
netstat -tulpn | grep 8082                 # 查看端口
tail -f logs/app.log                       # 实时日志
```

---

**交接完成日期**: 2026-04-21  
**交接文档版本**: v1.0.0  
**有效期**: 正式上线至系统维护期结束
