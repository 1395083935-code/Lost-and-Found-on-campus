# 🔐 校园失物招领平台 - 安全审计报告

**审计日期**: 2026年4月17日  
**审计范围**: 5个Controller的权限控制机制  
**审计结果**: ❌ **发现6个安全漏洞**

---

## 📋 审计结果汇总

| Controller | 状态 | 漏洞数 | 严重级别 | 说明 |
|-----------|------|-------|---------|------|
| ItemController | ✅ | 0 | - | 已正确实现，从认证上下文获取userId |
| UserController | ⚠️ | 2 | **高** | 权限验证缺失，任意用户可查看他人信息 |
| FavoriteController | 🔴 | 4 | **严重** | 直接信任客户端userId，任意用户可操作他人收藏 |
| FileController | ⚠️ | 1 | **中** | 缺少用户身份记录 |
| AdminController | ⚠️ | 1 | **中** | 缺少管理员权限检查 |

---

## 🔴 详细漏洞分析

### **1. UserController - 权限验证缺失**

#### 漏洞1.1: 任意用户查看他人信息

**代码位置**: [UserController.java](d:\桌面\workspace\campus-backend\src\main\java\com\campuslostfound\controller\UserController.java#L86)

```java
❌ 不安全的实现
@GetMapping("/user/info")
public Result<User> getUserInfo(@RequestParam Integer id) {
    User user = userService.getById(id);  // 直接信任客户端的id参数！
    if (user == null) {
        return Result.error("用户不存在");
    }
    user.setPassword(null);
    return Result.success(user);
}
```

**攻击场景**:
```bash
# 任何用户都可以查看任何其他用户的信息
curl -H "Authorization: Bearer user2_token" \
     "http://localhost:8082/api/user/info?id=1"
# 返回user_id=1的所有信息（邮箱、电话、个人资料等）
```

**风险**: 隐私泄露，信息收集

**修复方案**: 只允许用户查看自己的信息或从token中的userId获取

---

#### 漏洞1.2: logout端点接收未验证的userId

**代码位置**: [UserController.java](d:\桌面\workspace\campus-backend\src\main\java\com\campuslostfound\controller\UserController.java#L91)

```java
❌ 不必要的参数
@PostMapping("/auth/logout")
public Result<Boolean> logout(@RequestParam(required = false) Integer id) {
    // 虽然logout只返回true，但接收id参数易导致混淆和安全问题
    return Result.success(true);
}
```

**问题**: 虽然logout本身是无状态的，但接收未验证的参数不是好设计

**修复方案**: 删除id参数，从token中提取userId或直接忽略

---

### **2. FavoriteController - 严重权限漏洞** 🔴

#### 漏洞2.1: 任意用户代替他人收藏

**代码位置**: [FavoriteController.java](d:\桌面\workspace\campus-backend\src\main\java\com\campuslostfound\controller\FavoriteController.java#L23)

```java
❌ 严重漏洞：直接信任客户端的userId
@PostMapping("/favorites")
public Result<Favorite> addFavorite(@RequestBody Favorite favorite) {
    if (favorite == null || favorite.getUserId() == null || favorite.getItemId() == null) {
        return Result.error("参数不完整");
    }
    // 直接使用客户端发送的userId！
    QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
    wrapper.eq("user_id", favorite.getUserId())  // ❌ 漏洞
           .eq("item_id", favorite.getItemId());
    // ...
}
```

**攻击场景**:
```bash
# 用户A的token，但操作用户B的收藏
curl -X POST \
  -H "Authorization: Bearer userA_token" \
  -H "Content-Type: application/json" \
  -d '{"userId": 2, "itemId": 999}' \
  "http://localhost:8082/api/favorites"

# 用户B的收藏列表中就多了这个物品，而用户B不知道！
```

**风险**: 账户劫持、数据篡改、用户困惑

---

#### 漏洞2.2: 任意用户查看他人收藏列表

**代码位置**: [FavoriteController.java](d:\桌面\workspace\campus-backend\src\main\java\com\campuslostfound\controller\FavoriteController.java#L48)

```java
❌ 权限验证缺失
@GetMapping("/favorites")
public Result<Page<LostFoundItem>> getFavorites(
        @RequestParam Integer userId,  // ❌ 直接信任客户端的userId
        @RequestParam(defaultValue = "1") int current,
        @RequestParam(defaultValue = "10") int size) {
    QueryWrapper<Favorite> favoriteWrapper = new QueryWrapper<>();
    favoriteWrapper.eq("user_id", userId);  // ❌ 任意用户可查看任何userId的收藏
    // ...
}
```

**攻击场景**:
```bash
# 用户A查看用户B的全部收藏
curl -H "Authorization: Bearer userA_token" \
     "http://localhost:8082/api/favorites?userId=2&current=1&size=100"
# 获取用户B收藏的所有物品列表（隐私泄露）
```

**风险**: 隐私泄露、用户行为分析

---

#### 漏洞2.3: 任意用户删除他人收藏

**代码位置**: [FavoriteController.java](d:\桌面\workspace\campus-backend\src\main\java\com\campuslostfound\controller\FavoriteController.java#L76)

```java
❌ 权限验证缺失
@DeleteMapping("/favorites/{itemId}")
public Result<Void> removeFavorite(@PathVariable Long itemId, @RequestParam Integer userId) {
    QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
    wrapper.eq("user_id", userId)  // ❌ 直接信任客户端的userId
           .eq("item_id", itemId);
    boolean removed = favoriteService.remove(wrapper);
    // ...
}
```

**攻击场景**:
```bash
# 用户A删除用户B的某个收藏
curl -X DELETE \
  -H "Authorization: Bearer userA_token" \
  "http://localhost:8082/api/favorites/999?userId=2"

# 用户B的收藏被删除，不知道是谁干的
```

**风险**: 数据破坏、拒绝服务

---

#### 漏洞2.4: 任意用户检查他人的收藏

**代码位置**: [FavoriteController.java](d:\桌面\workspace\campus-backend\src\main\java\com\campuslostfound\controller\FavoriteController.java#L88)

```java
❌ 权限验证缺失
@GetMapping("/favorites/check")
public Result<Boolean> checkFavorite(@RequestParam Integer userId, @RequestParam Long itemId) {
    QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
    wrapper.eq("user_id", userId)  // ❌ 直接信任客户端userId
           .eq("item_id", itemId);
    // ...
}
```

**攻击场景**:
```bash
# 用户A探测用户B是否收藏了某个物品
for itemId in $(seq 1 1000); do
  curl "http://localhost:8082/api/favorites/check?userId=2&itemId=$itemId"
done
# 可以推断出用户B的兴趣和行为
```

**风险**: 用户隐私泄露、行为分析

---

### **3. FileController - 缺少用户身份记录**

#### 漏洞3.1: 上传文件未记录用户身份

**代码位置**: [FileController.java](d:\桌面\workspace\campus-backend\src\main\java\com\campuslostfound\controller\FileController.java#L25)

```java
@PostMapping("/upload")
public Result<String> uploadFile(@RequestParam("file") MultipartFile file,
                                 HttpServletRequest request) {
    // 虽然有request参数，但没有验证用户身份
    // 无法追踪谁上传了什么文件
    // ...
}
```

**问题**: 
- 无法审计谁上传了哪些文件
- 如果发现恶意内容，无法追责
- 存储空间消耗无法归属到用户

**修复方案**: 验证用户身份，在文件存储时记录userId

---

### **4. AdminController - 缺少权限检查**

#### 漏洞4.1: 统计接口任何认证用户都可访问

**代码位置**: [AdminController.java](d:\桌面\workspace\campus-backend\src\main\java\com\campuslostfound\controller\AdminController.java#L20)

```java
❌ 缺少管理员检查
@GetMapping("/stats")
public Result<Map<String, Long>> stats() {
    // 任何认证用户都可以看到全平台统计数据
    long totalItems = itemService.count();
    long pendingReview = itemService.lambdaQuery().eq(LostFoundItem::getStatus, 2).count();
    // ...
}
```

**问题**: 普通用户不应该看到平台统计数据

**修复方案**: 添加`@RequireRole("ADMIN")`或在方法中验证权限

---

## ✅ 已正确实现的例子

### ItemController - 安全的实现 ✅

```java
✅ 正确的模式：从认证上下文获取userId
@PutMapping("/{id:\\d+}")
public Result<LostFoundItem> updateItem(
        @PathVariable Long id, 
        @RequestBody LostFoundItem req, 
        HttpServletRequest request) {
    
    LostFoundItem item = itemService.getById(id);
    if (item == null) {
        return Result.error("信息不存在");
    }
    
    // ✅ 从认证上下文获取userId，不信任客户端
    Integer userId = getAuthenticatedUserId(request);
    
    // ✅ 权限验证：只有所有者可以编辑
    if (userId == null || !userId.equals(item.getUserId())) {
        return Result.error("无权编辑该信息");
    }
    
    // 安全的编辑逻辑
    // ...
}

private Integer getAuthenticatedUserId(HttpServletRequest request) {
    Object userId = request.getAttribute("userId");  // 从JwtInterceptor设置
    if (userId instanceof Integer) {
        return (Integer) userId;
    }
    return null;
}
```

---

## 📝 修复清单

| 优先级 | 文件 | 问题 | 修复方案 | 状态 |
|--------|------|------|---------|------|
| 🔴 严重 | FavoriteController | 4个权限漏洞 | 添加HttpServletRequest参数，验证userId | ⏳ |
| 🟠 高 | UserController | 权限验证缺失 | 删除id参数，只允许查看自己信息 | ⏳ |
| 🟡 中 | FileController | 缺少用户记录 | 从request获取userId，记录上传者 | ⏳ |
| 🟡 中 | AdminController | 缺少权限检查 | 添加管理员角色验证 | ⏳ |

---

## 🚀 修复执行计划

**修复顺序** (按优先级):
1. **FavoriteController** (严重) - 30分钟
2. **UserController** (高) - 15分钟  
3. **FileController** (中) - 15分钟
4. **AdminController** (中) - 10分钟
5. **编写测试用例验证** - 30分钟

**总耗时**: ~100分钟

---

## 📐 安全原则总结

修复时遵循以下原则：

```
❌ 不安全: Integer userId = request.getParameter("userId");
✅ 安全: Integer userId = (Integer) request.getAttribute("userId");  // 从JwtInterceptor

❌ 不安全: 在@RequestParam中接收userId
✅ 安全: 在HttpServletRequest中读取userId（由JwtInterceptor设置）

❌ 不安全: 信任客户端发送的userId
✅ 安全: 从token中解析userId，且验证权限

❌ 不安全: 任何认证用户都可以访问管理接口
✅ 安全: 检查user.getRole()是否为ADMIN/1
```

---

## 🔍 测试验证清单

修复后需要验证的内容：

```javascript
// 测试1: 用户A不能查看用户B的信息
GET /api/user/info  // 应该只返回自己的信息，不接受id参数

// 测试2: 用户A不能代替用户B收藏
POST /api/favorites
// userId应该从token自动获取，request body不包含userId

// 测试3: 用户A不能查看用户B的收藏
GET /api/favorites  // 不接受userId参数，自动获取当前用户

// 测试4: 用户A不能删除用户B的收藏
DELETE /api/favorites/{itemId}  // userId从token获取

// 测试5: 普通用户无法访问统计接口
GET /api/admin/stats  // 返回403 Forbidden
```

---

**下一步**: 开始实施修复计划
