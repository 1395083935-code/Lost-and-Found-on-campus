package com.campuslostfound.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campuslostfound.common.Result;
import com.campuslostfound.entity.LostFoundItem;
import com.campuslostfound.service.LostFoundItemService;
import com.campuslostfound.service.UserService;
import com.campuslostfound.utils.EncryptionUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private LostFoundItemService itemService;

    @Autowired
    private UserService userService;

    // 定义允许的分类值
    private static final java.util.Set<String> ALLOWED_CATEGORIES = java.util.Set.of(
        "证件", "电子产品", "文具饰品", "衣物箱包", "其他",
        "certificate", "electronic", "stationery", "clothing", "other",
        "钱包", "钥匙", "衣服", "箱包", "日用品"
    );

    private Integer getAuthenticatedUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId instanceof Integer) {
            return (Integer) userId;
        }
        if (userId instanceof Number) {
            return ((Number) userId).intValue();
        }
        return null;
    }

    // 1. 发布失物/招领
    @PostMapping
    public Result<LostFoundItem> publish(@RequestBody LostFoundItem item, HttpServletRequest request) {
        if (item == null
                || !StringUtils.hasText(item.getTitle())
                || !StringUtils.hasText(item.getLocation())
                || !StringUtils.hasText(item.getContactInfo())
                || item.getType() == null) {
            return Result.error("参数不完整");
        }
        Integer userId = getAuthenticatedUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }
        if (userService.getById(userId) == null) {
            return Result.error("登录状态无效，请重新登录");
        }

        // 防重复检测：检查1小时内是否有相同的发布
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long duplicateCount = itemService.lambdaQuery()
                .eq(LostFoundItem::getUserId, userId)
                .eq(LostFoundItem::getTitle, item.getTitle())
                .eq(LostFoundItem::getLocation, item.getLocation())
                .ge(LostFoundItem::getCreateTime, oneHourAgo)
                .count();
        if (duplicateCount > 0) {
            return Result.error("已存在相同信息，请勿重复发布");
        }

        // 每日发布限制：同一用户每天最多发布 5 条
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        long todayCount = itemService.lambdaQuery()
                .eq(LostFoundItem::getUserId, userId)
                .ge(LostFoundItem::getCreateTime, dayStart)
                .count();
        if (todayCount >= 5) {
            return Result.error("每日最多发布 5 条信息，请明天再试");
        }

        item.setUserId(userId);
        
        // 参数范围校验
        if (item.getTitle().length() > 20) {
            return Result.error("标题长度不能超过 20 字");
        }
        if (item.getDescription() != null && item.getDescription().length() > 500) {
            return Result.error("描述长度不能超过 500 字");
        }
        if (item.getCategory() != null && !ALLOWED_CATEGORIES.contains(item.getCategory())) {
            return Result.error("分类值不合法");
        }
        if (item.getLocation().length() > 255) {
            return Result.error("地点长度不能超过 255 字");
        }
        if (item.getContactInfo().length() > 255) {
            return Result.error("联系方式长度不能超过 255 字");
        }
        
        if (item.getType() == 1 && !StringUtils.hasText(item.getStorageLocation())) {
            return Result.error("拾物招领请填写存放地点");
        }
        if (item.getType() == 1 && item.getStorageLocation().length() > 255) {
            return Result.error("存放地点长度不能超过 255 字");
        }
        
        if (item.getStatus() == null) {
            item.setStatus(0);  // 发布时自动设置为待审核
        }
        if (item.getAnonymous() == null) {
            item.setAnonymous(0);  // 0表示非匿名
        }
        if (item.getCreateTime() == null) {
            item.setCreateTime(LocalDateTime.now());
        }
        item.setUpdateTime(LocalDateTime.now());
        
        // 匿名发布时隐藏 contactInfo
        if (item.getAnonymous() != null && item.getAnonymous() == 1) {
            item.setContactInfo(null);
        } else {
            // 非匿名发布时，加密 contactInfo
            String originalContact = item.getContactInfo();
            if (StringUtils.hasText(originalContact)) {
                item.setContactInfo(EncryptionUtils.encrypt(originalContact));
            }
        }
        
        boolean success = itemService.saveItem(item);
        if (success) {
            // 返回时脱敏contactInfo（显示脱敏版本）
            if (item.getContactInfo() != null && !item.getContactInfo().isEmpty()) {
                item.setContactInfo(EncryptionUtils.maskPhoneNumber(item.getContactInfo()));
            }
            return Result.success(item);
        } else {
            return Result.error("发布失败");
        }
    }

    // 2. 获取列表（支持 keyword/category/days 筛选）
    @GetMapping
    public Result<IPage<LostFoundItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer days) {
        IPage<LostFoundItem> result = itemService.listItems(page, size, type, keyword, category, status, days);
        return Result.success(result);
    }

    // 3. 获取个人发布列表（个人中心）
    @GetMapping("/my")
    public Result<List<LostFoundItem>> myItems(HttpServletRequest request) {
        Integer userId = getAuthenticatedUserId(request);
        if (userId == null || userService.getById(userId) == null) {
            return Result.error("登录状态无效，请重新登录");
        }
        List<LostFoundItem> list = itemService.listByUserId(userId);
        return Result.success(list);
    }

    // 4. 获取详情
    @GetMapping("/{id:\\d+}")
    public Result<LostFoundItem> detail(@PathVariable Long id) {
        LostFoundItem item = itemService.getById(id);
        if (item != null) {
            // 解密 contactInfo（如果是加密的）
            if (item.getContactInfo() != null && EncryptionUtils.isValidBase64(item.getContactInfo())) {
                try {
                    item.setContactInfo(EncryptionUtils.decrypt(item.getContactInfo()));
                } catch (Exception e) {
                    // 解密失败，保持原值
                }
            }
            return Result.success(item);
        } else {
            return Result.error("信息不存在");
        }
    }

    // 4. 更新状态（个人中心：标记完结）
    @PatchMapping("/{id:\\d+}/status")
    public Result<LostFoundItem> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body, HttpServletRequest request) {
        LostFoundItem item = itemService.getById(id);
        if (item == null) {
            return Result.error("信息不存在");
        }
        Integer userId = getAuthenticatedUserId(request);
        if (userId == null || !userId.equals(item.getUserId())) {
            return Result.error("无权操作该信息");
        }
        Integer status = body == null ? null : body.get("status");
        if (status == null || status < 0 || status > 3) {
            return Result.error("状态参数非法");
        }
        item.setStatus(status);
        item.setUpdateTime(LocalDateTime.now());
        boolean success = itemService.updateById(item);
        return success ? Result.success(item) : Result.error("状态更新失败");
    }

    // 5. 编辑并重提（个人中心：编辑重提）
    @PutMapping("/{id:\\d+}")
    public Result<LostFoundItem> updateItem(@PathVariable Long id, @RequestBody LostFoundItem req, HttpServletRequest request) {
        LostFoundItem item = itemService.getById(id);
        if (item == null) {
            return Result.error("信息不存在");
        }
        Integer userId = getAuthenticatedUserId(request);
        if (req == null || userId == null || !userId.equals(item.getUserId())) {
            return Result.error("无权编辑该信息");
        }
        if (req == null
                || !StringUtils.hasText(req.getTitle())
                || !StringUtils.hasText(req.getLocation())
                || !StringUtils.hasText(req.getContactInfo())
                || req.getType() == null) {
            return Result.error("参数不完整");
        }
        
        // 参数范围校验
        if (req.getTitle().length() > 20) {
            return Result.error("标题长度不能超过 20 字");
        }
        if (req.getDescription() != null && req.getDescription().length() > 500) {
            return Result.error("描述长度不能超过 500 字");
        }
        if (req.getCategory() != null && !ALLOWED_CATEGORIES.contains(req.getCategory())) {
            return Result.error("分类值不合法");
        }
        if (req.getLocation().length() > 255) {
            return Result.error("地点长度不能超过 255 字");
        }
        if (req.getContactInfo().length() > 255) {
            return Result.error("联系方式长度不能超过 255 字");
        }
        
        if (req.getType() == 1 && !StringUtils.hasText(req.getStorageLocation())) {
            return Result.error("拾物招领请填写存放地点");
        }
        if (req.getType() == 1 && req.getStorageLocation().length() > 255) {
            return Result.error("存放地点长度不能超过 255 字");
        }

        item.setTitle(req.getTitle());
        item.setDescription(req.getDescription());
        item.setType(req.getType());
        item.setCategory(req.getCategory());
        item.setLocation(req.getLocation());
        item.setStorageLocation(req.getStorageLocation());
        item.setAnonymous((req.getAnonymous() != null && req.getAnonymous() != 0) ? 1 : 0);
        item.setImageUrl(req.getImageUrl());
        item.setUserId(userId);
        
        // 匿名发布时隐藏 contactInfo
        if (item.getAnonymous() != null && item.getAnonymous() == 1) {
            item.setContactInfo(null);
        } else {
            item.setContactInfo(req.getContactInfo());
        }
        
        // 编辑重提后统一进入待审核
        item.setStatus(0);
        if (req.getCreateTime() != null) {
            item.setCreateTime(req.getCreateTime());
        }
        item.setUpdateTime(LocalDateTime.now());

        boolean success = itemService.updateById(item);
        return success ? Result.success(item) : Result.error("更新失败");
    }

    // 6. 审核操作（管理员：通过/驳回）
    @PatchMapping("/{id:\\d+}/review")
    public Result<LostFoundItem> review(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        // ✅ 修复：添加管理员权限验证
        Integer userId = getAuthenticatedUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        com.campuslostfound.entity.User admin = userService.getById(userId);
        if (admin == null || admin.getRole() == null || admin.getRole() != 1) {
            return Result.error("无权审核，仅管理员可操作");
        }
        
        LostFoundItem item = itemService.getById(id);
        if (item == null) {
            return Result.error("信息不存在");
        }
        Integer status = body == null || body.get("status") == null ? null : ((Number) body.get("status")).intValue();
        if (status == null || (status != 1 && status != 2)) {
            return Result.error("审核状态只能为通过(1)或驳回(2)");
        }
        String rejectReason = body == null || body.get("rejectReason") == null ? null : String.valueOf(body.get("rejectReason")).trim();
        if (status == 2 && !StringUtils.hasText(rejectReason)) {
            return Result.error("驳回时请选择驳回理由");
        }
        if (rejectReason != null && rejectReason.length() > 255) {
            return Result.error("驳回理由不能超过 255 字");
        }
        item.setStatus(status);
        item.setRejectReason(status == 2 ? rejectReason : null);
        item.setUpdateTime(LocalDateTime.now());
        boolean success = itemService.updateById(item);
        return success ? Result.success(item) : Result.error("审核失败");
    }

    // 7. 删除信息（个人中心：删除发布）
    @DeleteMapping("/{id:\\d+}")
    public Result<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        LostFoundItem item = itemService.getById(id);
        if (item == null) {
            return Result.error("信息不存在");
        }
        Integer userId = getAuthenticatedUserId(request);
        if (userId == null || !userId.equals(item.getUserId())) {
            return Result.error("无权删除该信息");
        }
        boolean success = itemService.removeById(id);
        return success ? Result.success(true) : Result.error("删除失败");
    }

} // 确保这里有一个结束的大括号，且文件到这里就结束了