校园失物招领小程序 - 全栈本地化技术实现文档
1. 核心原则 (Core Principles)
纯本地化 (100% Local): 所有服务（前端、后端、数据库、文件存储）均运行在本地开发环境，不依赖任何第三方云服务（如微信云开发、阿里云OSS等）。
成熟框架优先 (Mature Frameworks): 优先选择生态完善、文档丰富、Cursor 识别度高的框架，避免“手搓”轮子。
RESTful 架构: 前后端分离，通过标准 HTTP 接口通信。
2. 技术栈选型 (Tech Stack)
为了满足“不手搓代码”的要求，我们将使用以下开源组件：
| 层级 | 技术/框架 | 选择理由 (Prompt Cursor 使用) |
| :--- | :--- | :--- |
| 小程序端 | UniApp (Vue 3 + Vite) | 官方推荐跨平台方案，生态极丰富，Cursor 对 Vue 3 支持最好。 |
| UI 组件库 | uView Plus 3.0 | 关键点：这是 UniApp 生态中最成熟的开源组件库。使用它可以让 Cursor 直接生成带有样式的代码（如搜索框、卡片、弹窗），无需写 CSS。 |
| 后端语言 | Java (OpenJDK 17) | 企业级开发首选，Spring Boot 生态极其稳定，适合处理业务逻辑。 |
| 后端框架 | Spring Boot 3.x | 成熟的依赖注入和自动配置，拥有海量的 Starter 组件。 |
| ORM 框架 | MyBatis-Plus | 简化数据库操作，提供代码生成器，减少手写 SQL。 |
| 数据库 | MySQL 8.0 | 文档中指定的轻量级关系型数据库，本地安装简单。 |
| 本地文件存储 | 本地磁盘 (Local Disk) | 关键点：替代云存储。后端接收图片后，保存在本地 `D:/upload/` (或项目内) 目录，通过本地 HTTP 接口访问。 |
| 接口文档 | Knife4j (OpenAPI 3) | 本地化的 API 文档界面，方便调试，无需联网。 |
| 开发工具 | Cursor (Full Stack) | 利用 AI 生成代码。 |

3.后端详细实现方案 (Spring Boot)
项目结构：
src/main/java/com/campuslostfound
├── controller       # 接收小程序请求 (对应文档 6. 接口规范)
├── service          # 业务逻辑 (对应文档 3.x 功能逻辑)
├── mapper           # 数据库映射 (对应文档 5. 数据库设计)
├── entity           # 实体类 (User, LostFound)
├── config           # 配置类 (跨域、文件上传路径)
└── CampusApplication.java
关键配置 (application.yml):
server:
  port: 8080 # 后端本地端口

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_db?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
  servlet:
    multipart:
      max-file-size: 10MB # 允许上传最大文件
      max-request-size: 10MB

# 本地文件上传配置 (重要)
file:
  upload-path: D:/campus_uploads/ # 本地存储路径，需提前创建文件夹
  access-path: /upload/** # 访问映射路径
文件上传逻辑 (不使用云存储):
Cursor 需要生成一个 FileController。
小程序上传图片 -> 后端接口。
后端将文件保存到 D:/campus_uploads/2024/04/05/xxx.jpg。
后端返回 URL：http://localhost:8080/upload/2024/04/05/xxx.jpg。
小程序直接显示该 URL。
4.前端详细实现方案 (UniApp + uView)
项目初始化:
在 Cursor 中执行: npx degit dcloudio/uni-preset-vue#vite my-project (创建 Vite + Vue 3 模板)。
uView 安装配置:
下载 uView UI 库源码放入 uni_modules 文件夹（UniApp 官方插件市场下载）。
在 main.js 中引入 uView。
在 App.vue 中引入 uView 样式。
利用 uView 组件 (Cursor Prompt 指令):
告诉 Cursor 使用 uView 的组件来实现 UI，例如：
搜索框: 使用 <u-search>。
卡片: 使用 <u-card> 或 <u-cell>。
发布按钮: 使用 <u-button type="primary" shape="circle">。
图片上传: 使用 <u-upload> (配置 action 为本地 http://localhost:8080/api/file/upload)。
5.数据库初始化脚本
在 MySQL 中执行以下 SQL，创建本地数据库：
CREATE DATABASE IF NOT EXISTS campus_db DEFAULT CHARSET utf8mb4;
USE campus_db;

-- 用户表 (对应文档 5.1)
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `openid` varchar(64) DEFAULT NULL COMMENT '微信OpenID',
  `nickname` varchar(50) DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `status` tinyint DEFAULT '1' COMMENT '1正常 0封禁',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 失物招领表 (对应文档 5.2)
CREATE TABLE `lost_found` (
  `id` int NOT NULL AUTO_INCREMENT,
  `type` tinyint NOT NULL COMMENT '1失物 2拾物',
  `title` varchar(100) NOT NULL,
  `category` varchar(20) NOT NULL,
  `location` varchar(100) NOT NULL,
  `description` text,
  `images` text COMMENT '本地存储的URL，逗号分隔',
  `contact` varchar(20),
  `status` tinyint DEFAULT '0' COMMENT '0待审核 1已通过 2已驳回',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
Cursor 开发指令建议 (Prompt Engineering)
6.你可以直接将以下指令复制给 Cursor，让它开始工作：
创建后端项目:
"使用 Spring Boot 3, MyBatis-Plus, MySQL 8 创建一个后端项目。实现用户管理、失物招领信息的 CRUD 接口。要求使用 Knife4j 生成 API 文档。数据库连接使用本地配置。"
实现文件上传:
"在 Spring Boot 中写一个文件上传接口。要求：接收 multipart/form-data，将文件保存到本地磁盘 D:/campus_uploads/ 目录下，并返回本地访问 URL (http://localhost:8080/upload/...)。"
创建前端页面:
"使用 UniApp Vue3 和 uView Plus UI 库，创建一个校园失物招领小程序的首页。使用 uView 的 u-tabs, u-search, u-cell, u-button 组件。页面包含顶部搜索、分类标签和瀑布流列表。"
连接接口:
"在 UniApp 中封装一个 request.js 工具类，配置 baseURL 为 http://10.0.2.2:8080 (Android 模拟器访问本地电脑) 或 http://127.0.0.1:8080 (真机调试需配置 hosts 或局域网 IP)。调用后端的 /api/lost/list 接口获取数据并渲染到首页列表。"