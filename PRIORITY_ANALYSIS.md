# 🎯 校园失物招领项目 - 优先级任务分析

**分析时间**: 2026-04-17  
**当前完成度**: 52% → 目标 85%  
**预计周期**: 7-10 天

---

## 📊 快速概览

| 优先级 | 任务类别 | 工作量 | 关键性 | 建议周期 |
|--------|---------|--------|--------|---------|
| 🔴 P0 | 数据库初始化 + 表结构调整 | 2h | **核心** | D1 |
| 🔴 P0 | 管理员审核流程（后端） | 4h | **核心** | D2-D3 |
| 🔴 P0 | 前端表单完整验证 | 3h | **核心** | D3-D4 |
| 🟠 P1 | 搜索+筛选功能完善 | 2.5h | **重要** | D4 |
| 🟠 P1 | 图片上传功能完整化 | 3h | **重要** | D5 |
| 🟡 P2 | 前端样式优化（整体体验） | 3h | **中等** | D5-D6 |
| 🟡 P2 | 管理后台完整实现 | 4h | **中等** | D6-D7 |
| 🟢 P3 | 消息通知系统（可选） | 3h | **低** | D8+ |
| 🟢 P3 | 数据统计报表（可选） | 2h | **低** | D8+ |

---

## 🔴 **P0 优先级任务（核心必做）**

### **任务 1: 数据库初始化与表结构调整** ⏱️ 2h

**当前状态**: 表结构已规划，未初始化

**工作清单**:
1. **数据库初始化**
   - [ ] MySQL 创建数据库：`campus_db`
   - [ ] 执行 SQL 初始化脚本（见下方）
   
2. **表结构补充**
   - [ ] User 表：添加 `student_id`（学号）字段
   - [ ] LostFoundItem 表：添加 `reject_reason`（驳回原因）字段
   - [ ] LostFoundItem 表：添加 `is_anonymous`（匿名标识）字段
   - [ ] LostFoundItem 表：修改 `status` 字段注释（0待审核, 1已通过, 2已驳回, 3已完结）
   - [ ] 创建 Notice 表（消息通知）
   - [ ] 创建 Admin 表（管理员）
   - [ ] 创建 Favorite 表（收藏关系）- 若无

3. **ORM 实体类同步**
   - [ ] 更新 User.java（添加 studentId）
   - [ ] 更新 LostFoundItem.java（添加 rejectReason, isAnonymous, storageLocation）
   - [ ] 创建 Notice.java
   - [ ] 创建 Admin.java
   - [ ] 创建 Favorite.java（若无）

**实现要点**:
```sql
-- 必须补充的字段
ALTER TABLE user ADD COLUMN student_id VARCHAR(20) COMMENT '学号/工号';
ALTER TABLE lost_found_item ADD COLUMN reject_reason VARCHAR(255) COMMENT '驳回原因';
ALTER TABLE lost_found_item ADD COLUMN is_anonymous TINYINT DEFAULT 0 COMMENT '匿名发布:1是,0否';
ALTER TABLE lost_found_item ADD COLUMN storage_location VARCHAR(100) COMMENT '存放地点(仅拾物)';
```

**关键依赖**: ✅ 所有后端功能都依赖这个

---

### **任务 2: 管理员审核流程（后端）** ⏱️ 4h

**当前状态**: AdminController 存在但功能不完整

**工作清单**:
1. **后端 API 实现**
   - [ ] 待审核列表接口：`GET /api/admin/pending` 
   - [ ] 通过审核接口：`PATCH /api/admin/{id}/approve`
   - [ ] 驳回审核接口：`PATCH /api/admin/{id}/reject` + 驳回原因
   - [ ] 批量审核接口：`POST /api/admin/batch-action`
   - [ ] 管理员登录接口：`POST /api/admin/login`

2. **数据库操作**
   - [ ] ItemService 添加 `updateStatus()` 方法
   - [ ] ItemService 添加 `listPendingItems()` 方法
   - [ ] 审核时自动生成 Notice 通知

3. **权限控制**
   - [ ] JwtInterceptor 添加管理员权限判断
   - [ ] 管理后台接口仅管理员可访问

4. **业务逻辑**
   - [ ] 通过审核 → 状态变 1，发送通知给发布者
   - [ ] 驳回审核 → 状态变 2，保存驳回原因，发送通知
   - [ ] 自动清理 30 天未审核的信息

**实现要点**:
```java
// AdminController 新增
@PostMapping("/approve/{id}")
public Result approve(@PathVariable int id) {
  // 状态改为 1（已通过）
  // 创建 Notice 记录
  // 返回成功
}

@PostMapping("/reject/{id}")
public Result reject(@PathVariable int id, @RequestParam String reason) {
  // 状态改为 2（已驳回）
  // 保存驳回原因
  // 创建 Notice 记录
}
```

**关键依赖**: 
- ✅ 依赖任务 1（数据库）
- 前端审核页面需要这些接口

---

### **任务 3: 前端表单完整验证** ⏱️ 3h

**当前状态**: Publish.vue 已有基础验证，但不完整

**工作清单**:
1. **Publish.vue 表单验证**
   - [ ] 物品名称：2-20 字（前端 maxlength + 后端校验）
   - [ ] 丢失时间：不可选未来时间
   - [ ] 地点：必填，支持快捷选择（教学楼/食堂/图书馆/操场/宿舍/体育馆）
   - [ ] 分类：6 个分类必须全覆盖（证件/电子/文具/衣物/其他）
   - [ ] 描述：0-500 字
   - [ ] 联系方式：手机号格式校验（1[3-9]\\d{9}）
   - [ ] 图片：1-3 张，每张 ≤ 2M
   - [ ] 每日发布限制：同一用户 ≤ 5 条（后端检查）

2. **Detail.vue 显示完整性**
   - [ ] 手机号脱敏：显示 1234****6789 格式
   - [ ] 匿名发布：隐藏头像昵称，仅显示联系方式
   - [ ] 状态标签：审核中/待认领/已找回/已归还/已驳回

3. **错误提示**
   - [ ] 表单错误显示清晰的 toast 提示
   - [ ] 上传失败重试机制

**实现要点**:
```javascript
// Publish.vue 中
const validateForm = () => {
  if (!form.title || form.title.length < 2 || form.title.length > 20) {
    message.error('物品名称2-20字');
    return false;
  }
  if (!/^1[3-9]\d{9}$/.test(form.contactInfo)) {
    message.error('请输入有效手机号');
    return false;
  }
  // ... 其他验证
  return true;
}
```

**关键依赖**:
- ✅ 依赖任务 1（数据库）
- ✅ 依赖任务 2（审核流程）

---

## 🟠 **P1 优先级任务（重要功能）**

### **任务 4: 搜索 + 筛选完善** ⏱️ 2.5h

**当前状态**: ItemService 有基础查询，前端筛选 UI 有但逻辑不完整

**工作清单**:
1. **后端 API**
   - [ ] 完善 `GET /api/items?keyword=xxx&category=xxx&days=xxx` 接口
   - [ ] 按 category（分类）过滤
   - [ ] 按 days（时间范围）过滤：1/3/7/all
   - [ ] 按 keyword（关键词）搜索：title + description + location
   - [ ] 排序：最新 / 相关性

2. **前端实现**
   - [ ] App.vue：实现分类筛选选项卡
   - [ ] App.vue：实现时间筛选按钮
   - [ ] App.vue：搜索框联动到详细搜索页
   - [ ] 显示"共 X 条"统计

3. **业务规则**
   - [ ] 只显示 status=1（已通过）的信息
   - [ ] 30 天前的信息自动隐藏

**实现要点**:
```javascript
// 后端 ItemService
public List<LostFoundItem> search(String keyword, String category, Integer days) {
  QueryWrapper<LostFoundItem> qw = new QueryWrapper<>();
  qw.eq("status", 1) // 只显示已通过
    .and(w -> w.like("title", keyword).or().like("description", keyword))
    .eq("category", category);
  
  if (days != null) {
    LocalDateTime since = LocalDateTime.now().minusDays(days);
    qw.ge("create_time", since);
  }
  return itemMapper.selectList(qw);
}
```

**关键依赖**: ✅ 依赖任务 1, 2

---

### **任务 5: 图片上传功能完整化** ⏱️ 3h

**当前状态**: FileController 有基础上传，但路径处理、校验不完整

**工作清单**:
1. **后端 FileController**
   - [ ] 检查文件大小（≤ 2M）
   - [ ] 检查文件类型（仅jpg/png/webp）
   - [ ] 生成日期路径：`/upload/2024/04/17/xxx.jpg`
   - [ ] 自动压缩图片（可选，用 ImageMagick）
   - [ ] 返回访问 URL：`http://localhost:8082/upload/...`

2. **前端 Publish.vue**
   - [ ] 图片预览（1-3 张）
   - [ ] 删除已上传的图片
   - [ ] 上传进度条
   - [ ] 图片大小校验（前端 + 后端）

3. **配置调整**
   - [ ] application.yml：配置本地存储路径
   - [ ] Spring 静态资源映射：`/upload/` → `D:/campus_uploads/`

4. **测试**
   - [ ] 上传图片后能正常显示
   - [ ] 发布时图片 URL 能保存到数据库

**实现要点**:
```java
// FileController
@PostMapping("/upload")
public Result<String> upload(MultipartFile file) {
  if (file.getSize() > 2 * 1024 * 1024) {
    return Result.error("文件过大");
  }
  String filename = UUID.randomUUID() + ".jpg";
  String filepath = "D:/campus_uploads/" + LocalDate.now() + "/" + filename;
  file.transferTo(new File(filepath));
  return Result.success("http://localhost:8082/upload/" + filename);
}
```

**关键依赖**: ✅ 依赖任务 1

---

## 🟡 **P2 优先级任务（体验优化）**

### **任务 6: 前端样式优化** ⏱️ 3h

**当前状态**: 核心样式完成（20KB CSS），但细节待打磨

**工作清单**:
1. **首页优化**
   - [ ] 信息卡片悬浮效果
   - [ ] 下拉刷新 / 上拉加载动画
   - [ ] 空状态提示美化
   - [ ] 浮动发布按钮动画

2. **发布页优化**
   - [ ] 表单字段间距调整
   - [ ] 快捷选项样式美化
   - [ ] 图片上传区域优化
   - [ ] 提交按钮的 loading 状态

3. **详情页优化**
   - [ ] 图片轮播效果
   - [ ] 操作按钮排列（拨号/分享/收藏/举报）
   - [ ] 状态标签颜色区分

4. **个人中心优化**
   - [ ] 标签页切换动画
   - [ ] 我的发布列表样式
   - [ ] 收藏列表优化

5. **响应式**
   - [ ] 平板 (768px) 适配
   - [ ] 深色模式支持（可选）

**实现要点**:
```css
/* 信息卡片悬浮效果 */
.item-card {
  transition: transform 0.2s, box-shadow 0.2s;
}
.item-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0,0,0,0.1);
}
```

**关键依赖**: ✅ 依赖任务 1, 2, 3

---

### **任务 7: 管理后台完整实现** ⏱️ 4h

**当前状态**: Admin.vue 已有框架，但功能不完整

**工作清单**:
1. **Admin.vue 功能完善**
   - [ ] 待审核列表：获取 status=0 的信息
   - [ ] 审核操作：通过 / 驳回 + 原因输入
   - [ ] 批量审核：勾选多条后批量处理
   - [ ] 信息管理：编辑 / 删除 / 置顶
   - [ ] 用户管理：查看用户、封禁用户
   - [ ] 举报处理：查看举报、处理举报

2. **后端 API 补全**
   - [ ] 举报列表：`GET /api/admin/reports`
   - [ ] 处理举报：`POST /api/admin/reports/{id}/handle`
   - [ ] 用户列表：`GET /api/admin/users`
   - [ ] 封禁用户：`PATCH /api/admin/users/{id}/ban`
   - [ ] 公告管理：`GET/POST/PATCH /api/admin/announcements`

3. **权限保护**
   - [ ] 只有管理员能访问 /api/admin 接口
   - [ ] JWT token 中包含 role 字段

**实现要点**:
```java
// AdminController 补全
@GetMapping("/pending")
public Result<List<LostFoundItem>> getPendingItems() {
  return Result.success(itemService.lambdaQuery()
    .eq(LostFoundItem::getStatus, 0)
    .orderByDesc(LostFoundItem::getCreateTime)
    .list());
}
```

**关键依赖**: ✅ 依赖任务 1, 2, 4

---

## 🟢 **P3 优先级任务（可选增强）**

### **任务 8: 消息通知系统** ⏱️ 3h

**工作清单**:
- [ ] Notice 表设计和 ORM
- [ ] NoticeService 实现
- [ ] 审核结果自动生成通知
- [ ] Profile.vue 通知列表展示
- [ ] 已读 / 未读标识

**实现**:
- 审核通过 / 驳回时创建 Notice 记录
- Profile 页面查询 Notice 列表

---

### **任务 9: 数据统计报表** ⏱️ 2h

**工作清单**:
- [ ] 核心数据接口：`GET /api/admin/stats`
- [ ] 返回：今日/周/月发布量、完结率、用户数
- [ ] Admin.vue 展示统计卡片

---

## 📋 **建议执行计划**

### **第 1 天（D1）**
✅ 完成任务 1：数据库初始化 + 表结构  
⏱️ 预计 2h  
📌 检查：MySQL 数据库创建，所有表正确初始化

### **第 2-3 天（D2-D3）**
✅ 完成任务 2：管理员审核流程（后端）  
⏱️ 预计 4h  
📌 检查：AdminController API 完整，审核流程可用

### **第 3-4 天（D3-D4）**
✅ 完成任务 3：前端表单完整验证  
⏱️ 预计 3h  
📌 检查：Publish.vue 表单校验完整，Detail.vue 显示正确

### **第 4 天（D4）**
✅ 完成任务 4：搜索 + 筛选 (2026-04-18 00:41:48) [详见 TASK_4_COMPLETION_REPORT.md]  
⏱️ 预计 2.5h  
📌 检查：首页搜索筛选功能正常

### **第 5 天（D5）**
✅ 完成任务 5：图片上传  
⏱️ 预计 3h  
📌 检查：发布时能上传图片，Detail 能显示

### **第 5-6 天（D5-D6）**
✅ 完成任务 6：样式优化  
⏱️ 预计 3h  
📌 检查：UI 美观，交互流畅

### **第 6-7 天（D6-D7）**
✅ 完成任务 7：管理后台  
⏱️ 预计 4h  
📌 检查：管理员能审核、管理信息

### **第 8 天+（可选）**
✅ 任务 8-9：通知 + 统计  
⏱️ 预计 5h  
📌 可选功能，增强体验

---

## 🎯 **关键成功指标**

| 检查项 | 完成标准 |
|--------|---------|
| 数据库 | 所有表创建完成，无错误 |
| 审核流程 | 管理员能通过/驳回信息，发布者收到通知 |
| 发布功能 | 表单完整校验，图片正常上传显示 |
| 搜索筛选 | 能按分类/时间/关键词搜索 |
| 管理后台 | 管理员能完整操作信息和用户 |
| 整体体验 | 页面流畅，无明显 BUG，UI 美观 |

---

## ⚠️ **风险点**

| 风险 | 影响 | 建议 |
|------|------|------|
| 数据库初始化延迟 | 阻止所有后端开发 | **最优先完成** |
| 图片上传路径错误 | 图片无法显示 | 测试多种场景 |
| 前端表单验证不完整 | 垃圾数据进库 | 后端 + 前端双重校验 |
| 管理员权限控制遗漏 | 安全隐患 | 每个接口加权限检查 |

---

## 📞 **需要帮助？**

- **Q: 从哪里开始？** → 从任务 1（数据库）开始，这是所有其他任务的基础
- **Q: 优先级如何调整？** → 如果时间紧张，可先做 P0 + P1，P2/P3 可作为美化步骤
- **Q: 如何验证完成？** → 每个任务都有检查清单，完成后逐项验证

