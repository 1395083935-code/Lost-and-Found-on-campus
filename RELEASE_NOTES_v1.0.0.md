# Release Notes — v1.0.0

发布日期：2026-04-20  
仓库：https://github.com/1395083935-code/Lost-and-Found-on-campus

---

## 📦 本次发布内容

### 后端（Spring Boot 3.1 · Java 17）
- RESTful API 全量实现（用户、物品、评论、通知、管理员）
- JWT 登录鉴权 + Spring Security 访问控制
- 本地文件上传（`D:/campus_uploads/`），接口 `/api/file/upload`
- MyBatis-Plus ORM，MySQL 8.0 数据库
- Spring Boot Actuator 健康检查端点 `/actuator/health`
- 每日通知自动清理定时任务

### 前端（Vue 3 · Vue CLI 5）
- 首页：物品列表、搜索、分类/时间筛选、分页加载
- 发布页：失物 / 拾物表单（图片上传、地点快捷选项）
- 详情页：完整信息、联系方式脱敏展示
- 个人中心：我的发布管理、状态流转
- 管理员后台：审核、公告、用户管理、数据统计

### 基础设施
- GitHub Actions CI：后端 Maven 构建 + 前端 Vue CLI 构建
- `.gitignore` 覆盖 node_modules / target / dist / logs
- README 快速启动指南

---

## 🚀 快速启动

### 前置条件
| 组件 | 版本 |
|------|------|
| Java | 17+ |
| Node.js | 18+ |
| MySQL | 8.0 |

### 1. 数据库初始化
```sql
CREATE DATABASE IF NOT EXISTS campus_db DEFAULT CHARSET utf8mb4;
```

### 2. 启动后端
```bash
cd campus-backend
mvn spring-boot:run
# 或使用编译好的 JAR
java -jar target/campus-backend-0.0.1-SNAPSHOT.jar
```
后端默认端口：`8082`

### 3. 启动前端
```bash
cd campus-frontend
npm install
npm run serve
```
前端默认端口：`8081`，访问 http://localhost:8081

---

## 🔗 接口文档
启动后端后访问：http://localhost:8082/doc.html（Knife4j Swagger UI）

## 📁 文件上传
上传的文件保存在 `D:/campus_uploads/`，访问地址：`http://localhost:8082/upload/{年}/{月}/{日}/{文件名}`

---

## ⚠️ 已知限制
- 本版本为本地开发版，尚未配置生产环境部署（计划 5/1）
- 微信登录使用模拟 openid，需接入真实微信公众平台后替换
- 文件存储为本地磁盘，生产环境建议迁移至对象存储
