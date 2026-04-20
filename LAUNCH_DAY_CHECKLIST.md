# 🚀 冲刺启动跑清单（4月21日）

## 前置条件检查 ✅

### 环境准备（需要在4月20日完成）

```bash
# 1. Java版本检查
java -version
# 预期: openjdk version "17.x.x"

# 2. Maven版本检查
mvn -version
# 预期: Maven 3.6.0+

# 3. MySQL连接验证
mysql -uroot -p123456 -e "SELECT VERSION();"
# 预期: MySQL 8.0.x

# 4. 数据库检查
mysql -uroot -p123456 -e "USE campus_db; SHOW TABLES; SHOW CREATE TABLE user;"
# 预期: 7个表都存在，charset=utf8mb4

# 5. 后端编译验证
cd d:\桌面\workspace\campus-backend
mvn clean compile
# 预期: BUILD SUCCESS

# 6. 后端打包验证
mvn package
# 预期: campus-backend-0.0.1-SNAPSHOT.jar (64.8MB)

# 7. 前端构建验证
cd d:\桌面\workspace\campus-frontend
npm run build
# 预期: dist目录生成

# 8. 启动测试 (可选)
cd d:\桌面\workspace\campus-backend
java -jar target/campus-backend-0.0.1-SNAPSHOT.jar &
sleep 3
curl -s http://localhost:8082/doc.html | head -20
# 预期: Swagger API文档加载成功
```

---

## 🎯 启动日程（4月21日）

### 08:00-08:30 | 团队集结 & 计划同步
```
✓ 所有项目成员到位
✓ 分发冲刺计划文档 (SPRINT_DETAILED_EXECUTION_PLAN.md)
✓ 分发快速参考卡 (SPRINT_QUICK_REFERENCE.md)
✓ 分发每日清单模板 (DAILY_CHECKLIST_TEMPLATE.md)
✓ 分配角色职责:
  - 测试负责人: 负责集成测试
  - 数据库管理员: 负责DB部署与优化
  - 后端工程师: 负责性能优化与缺陷修复
  - 前端工程师: 负责前端集成与测试
  - 项目经理: 进度跟踪与协调
✓ 明确3周冲刺目标和5/7上线目标
```

### 08:30-09:00 | 环境启动验证
```bash
# 后端启动
cd d:\桌面\workspace\campus-backend
java -jar target/campus-backend-0.0.1-SNAPSHOT.jar &

# 等待3-5秒，检查日志
sleep 5
ps aux | grep java

# 前端启动 (如果需要)
cd d:\桌面\workspace\campus-frontend
npm run dev &

# 验证API可访问
curl -s http://localhost:8082/doc.html | grep "swagger" && echo "✅ 后端正常" || echo "❌ 后端异常"
```

**关键验证项**:
```
□ 后端Java进程启动成功
□ MySQL数据库连接成功
□ Swagger API文档可访问 (http://localhost:8082/doc.html)
□ 前端dist已构建或npm dev已启动
□ 没有关键错误日志
```

### 09:00-09:30 | 快速冒烟测试
```bash
# 测试1: 验证WeChat登录接口
curl -X POST http://localhost:8082/api/wechat/login \
  -H "Content-Type: application/json" \
  -d '{"code":"test_code"}' \
  -w "\nHTTP Status: %{http_code}\n"
# 预期: 200 或 400 (code无效但接口存在)

# 测试2: 验证物品列表接口
curl -X GET "http://localhost:8082/api/items?page=1&size=10" \
  -w "\nHTTP Status: %{http_code}\n"
# 预期: 200 或 401 (需登录但接口存在)

# 测试3: 验证搜索接口
curl -X GET "http://localhost:8082/api/search/items?keyword=手机" \
  -w "\nHTTP Status: %{http_code}\n"
# 预期: 200

# 测试4: 验证搜索热词接口
curl -X GET "http://localhost:8082/api/search/hotwords?limit=10" \
  -w "\nHTTP Status: %{http_code}\n"
# 预期: 200

# 如果全部通过，输出:
echo "✅ 所有冒烟测试通过，可以启动正式工作"
```

### 09:30-09:45 | 第一次晨会
```
报告项: 
  1. 环境启动状态 ✅/❌
  2. 冒烟测试结果 ✅/❌
  3. 是否有阻塞问题
  4. 今日任务开始信号 GO/NO-GO
```

### 09:45-10:00 | 分组配置

**分组建议**:
```
【集成测试组】
  - 一名QA或测试工程师
  - 一名前端工程师
  - 一名后端工程师 (应急支持)
  任务: 完成前后端集成测试 (任务1)
  工作地点: 测试环境
  检查点: SPRINT_DETAILED_EXECUTION_PLAN.md 第1部分

【数据库组】
  - 一名数据库管理员
  - 一名后端工程师
  任务: 部署生产DB与安全审计 (任务2)
  工作地点: 生产数据库
  检查点: SPRINT_DETAILED_EXECUTION_PLAN.md 第2部分

【性能优化组】
  - 一名后端工程师 (主)
  - 一名数据库工程师 (支持)
  任务: 性能优化与缺陷修复 (任务3/4)
  工作地点: 开发环境
  检查点: SPRINT_DETAILED_EXECUTION_PLAN.md 第4部分

【文档组】
  - 项目经理或技术文档工程师
  任务: 部署文档与监控配置 (任务5)
  工作地点: 文档系统
  检查点: SPRINT_DETAILED_EXECUTION_PLAN.md 第5部分
```

### 10:00 | 正式启动工作

```
🚀 开始执行5大任务！

【关键事项】
1. 每组选一人作为进度报告员
2. 每个小时报告一次进度 (15分钟)
3. 遇到问题立即升级到项目经理
4. 所有问题记录在缺陷数据库中
5. 代码提交前必须编译测试通过

【每小时进度报告格式】
时间: 11:00
组别: 集成测试组
完成: 登录接口测试完成
进行: 列表加载接口测试
阻塞: 无
下小时计划: 继续列表加载测试+发布接口测试
```

---

## 📋 启动前最后检查清单

```
【办公室准备】
□ 所有成员到位
□ 会议室已预定 (冲刺期间)
□ 足够的咖啡/茶/饮料
□ 临时白板和便签纸
□ 网络连接良好

【工具准备】
□ IDE (VS Code / IDEA) 已打开项目
□ Git已配置好username和email
□ MySQL工具 (Navicat/DBeaver) 已打开
□ Postman或Insomnia已导入API集合
□ 终端窗口已打开并指向项目根目录

【文档准备】
□ PRD.md (已阅读理解)
□ SPRINT_DETAILED_EXECUTION_PLAN.md (已打印或在线阅读)
□ SPRINT_QUICK_REFERENCE.md (已分发)
□ DAILY_CHECKLIST_TEMPLATE.md (已打印)
□ LANDING_ANALYSIS_SUMMARY.md (已阅读)
□ SPRINT_EXECUTION_CHECKLIST.md (已阅读)

【代码准备】
□ 所有代码已commit (没有uncommitted changes)
□ 主分支已更新到最新
□ 没有merge conflicts
□ .gitignore配置正确

【数据库准备】
□ 所有7个表已创建
□ 测试数据已初始化 (至少100条物品记录)
□ 索引优化脚本已准备 (optimize_indexes.sql)
□ 备份配置已准备 (setup_backup.sh)
□ 监控告警配置已准备 (prometheus.yml)

【系统准备】
□ 硬盘空间充足 (>50GB)
□ 内存充足 (>8GB可用)
□ CPU监控工具已安装 (htop/Task Manager)
□ 网络带宽充足
□ 电源充足 (笔记本已充电或连接电源)
```

---

## ⚠️ 启动前可能的问题及应急方案

| 问题 | 症状 | 应急方案 |
|------|------|---------|
| 后端启动失败 | 无法连接8082端口 | 检查MySQL连接 → 查看日志 → 重新编译 |
| 数据库连接失败 | java.sql.SQLException | 检查MySQL服务是否运行 → 验证用户/密码 → 重启MySQL |
| Git冲突 | merge conflicts on branch | 联系最后提交者 → 手工解决冲突 → 重新编译 |
| 内存溢出 | OutOfMemoryError | 增加JVM堆内存: -Xmx2048m |
| 字符乱码 | 中文显示为????? | 检查MySQL charset=utf8mb4 → 重新建表 |
| 权限拒绝 | Permission denied on file | 检查文件权限 → chmod +x脚本 |
| 端口被占用 | Address already in use | 杀死占用进程 → 改用其他端口 |
| 网络超时 | Connection timeout | 检查网络 → 检查防火墙 → 检查服务可用性 |

---

## 📊 启动日进度目标

### 今日（4/21）期望完成

```
【集成测试】
目标: 完成5个核心API的端到端测试 (40%)
预期完成: 登录 ✅ 列表 ✅ 搜索 ✅ 发布接口启动 ⏳

【数据库部署】
目标: 建立生产数据库环境 (50%)
预期完成: 索引优化脚本执行 ⏳ 测试数据导入 ⏳

【缺陷修复】
目标: 建立缺陷追踪系统 (100%)
预期完成: 缺陷数据库创建 ✅ 第一批P0缺陷记录 ⏳

【性能优化】
目标: 基准测试完成 (20%)
预期完成: 性能测试工具配置 ✅ 基准数据采集 ⏳

【文档】
目标: 监控配置 (30%)
预期完成: Prometheus规则准备 ⏳ 日志配置准备 ⏳
```

---

## 🎯 成功指标 (Day 1)

```
✅ 所有关键员工准时到位
✅ 环境启动验证100%通过
✅ 冒烟测试100%通过
✅ 集成测试启动并至少完成1个API路径
✅ 没有P0阻塞问题
✅ 所有问题都被记录和分类
✅ 明天的工作计划已确定
```

---

## 📞 启动日联系方式

```
项目经理: _____________ (电话: __________)
技术总监: _____________ (电话: __________)
应急热线: _____________ (24小时)
```

---

## 💬 启动日备忘录

```
【重要提醒】
1. 保证sleep充足 - 冲刺会很密集
2. 定时休息 - 每2小时休息10分钟
3. 保持水分 - 多喝水
4. 及时沟通 - 不要隐瞒问题
5. 先质量后速度 - 不要赶工
6. 互相支持 - 这是团队项目

【禁止事项】
✗ 代码不编译就提交
✗ 改动别人的代码不通知
✗ 隐瞒问题希望自己解决
✗ 在没有备份的情况下删除数据
✗ 在晚上10点后还在改关键代码
✗ 跳过测试直接上线

【激励语】
🎯 3周冲刺，5/7上线
💪 我们已经完成了P0、P1、全部P2
🚀 最后的冲刺会决定整个项目的成败
👥 团队协作，众志成城
🏆 成功的上线，每个人都是英雄
```

---

**打印此清单，在4月21日08:00前分发给所有参与者**

启动确认: □ (团队确认无误，可以GO)

日期: ________  
签字: ________

