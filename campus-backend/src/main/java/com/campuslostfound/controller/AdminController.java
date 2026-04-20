package com.campuslostfound.controller;

import com.campuslostfound.common.Result;
import com.campuslostfound.entity.LostFoundItem;
import com.campuslostfound.entity.Notice;
import com.campuslostfound.entity.User;
import com.campuslostfound.mapper.NoticeMapper;
import com.campuslostfound.service.LostFoundItemService;
import com.campuslostfound.service.UserService;
import com.campuslostfound.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private LostFoundItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * 从HttpServletRequest中获取已认证的用户ID
     */
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

    /**
     * 检查当前用户是否为管理员
     * role == 1 表示管理员
     */
    private boolean isAdmin(User user) {
        return user != null && user.getRole() != null && user.getRole() == 1;
    }

    private String generateToken(Integer userId) {
        return JwtUtils.generateToken(userId);
    }

    private Result<User> validateAdmin(HttpServletRequest request) {
        Integer userId = getAuthenticatedUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }
        User user = userService.getById(userId);
        if (!isAdmin(user)) {
            return Result.error("无权访问，仅管理员可操作");
        }
        return Result.success(user);
    }

    private void createReviewNotice(LostFoundItem item, Integer status, String rejectReason) {
        if (item == null || item.getUserId() == null) {
            return;
        }
        Notice notice = new Notice();
        notice.setUserId(item.getUserId());
        notice.setItemId(item.getId());
        if (status == 1) {
            notice.setType(1);
            notice.setContent("您发布的信息《" + item.getTitle() + "》已审核通过");
        } else {
            notice.setType(2);
            String reason = StringUtils.hasText(rejectReason) ? rejectReason : "请完善信息后重新提交";
            notice.setContent("您发布的信息《" + item.getTitle() + "》已被驳回，原因：" + reason);
        }
        notice.setIsRead(0);
        notice.setCreateTime(LocalDateTime.now());
        noticeMapper.insert(notice);
    }

    private void autoCleanupStalePending() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(30);
        List<LostFoundItem> staleItems = itemService.lambdaQuery()
            .eq(LostFoundItem::getStatus, 0)
                .lt(LostFoundItem::getCreateTime, deadline)
                .list();
        if (staleItems.isEmpty()) {
            return;
        }
        for (LostFoundItem item : staleItems) {
            itemService.updateStatus(item.getId(), 2, "超过30天未审核，系统自动驳回");
            createReviewNotice(item, 2, "超过30天未审核，系统自动驳回");
        }
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> adminLogin(@RequestBody User loginRequest) {
        if (loginRequest == null
                || !StringUtils.hasText(loginRequest.getUsername())
                || !StringUtils.hasText(loginRequest.getPassword())) {
            return Result.error("用户名和密码不能为空");
        }

        User user = userService.lambdaQuery()
                .eq(User::getUsername, loginRequest.getUsername())
                .one();
        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return Result.error("用户名或密码错误");
        }
        if (!isAdmin(user)) {
            return Result.error("当前账号不是管理员");
        }

        User safeUser = new User();
        safeUser.setId(user.getId());
        safeUser.setUsername(user.getUsername());
        safeUser.setAvatar(user.getAvatar());
        safeUser.setRole(user.getRole());
        safeUser.setStatus(user.getStatus());

        Map<String, Object> data = new HashMap<>();
        data.put("user", safeUser);
        data.put("token", generateToken(user.getId()));
        return Result.success(data);
    }

    @GetMapping("/pending")
    public Result<List<LostFoundItem>> pending(HttpServletRequest request) {
        Result<User> authResult = validateAdmin(request);
        if (authResult.getCode() != 200) {
            return Result.error(authResult.getMsg());
        }

        autoCleanupStalePending();
        return Result.success(itemService.listPendingItems());
    }

    @PatchMapping("/{id}/approve")
    public Result<LostFoundItem> approve(@PathVariable Long id, HttpServletRequest request) {
        Result<User> authResult = validateAdmin(request);
        if (authResult.getCode() != 200) {
            return Result.error(authResult.getMsg());
        }

        LostFoundItem item = itemService.getById(id);
        if (item == null) {
            return Result.error("信息不存在");
        }
        if (item.getStatus() != null && item.getStatus() != 0) {
            return Result.error("仅待审核的信息可执行通过操作");
        }

        boolean updated = itemService.updateStatus(id, 1, null);
        if (!updated) {
            return Result.error("审核通过失败");
        }
        item.setStatus(1);
        item.setRejectReason(null);
        createReviewNotice(item, 1, null);
        return Result.success(item);
    }

    @PatchMapping("/{id}/reject")
    public Result<LostFoundItem> reject(@PathVariable Long id,
                                        @RequestBody Map<String, Object> body,
                                        HttpServletRequest request) {
        Result<User> authResult = validateAdmin(request);
        if (authResult.getCode() != 200) {
            return Result.error(authResult.getMsg());
        }

        String rejectReason = body == null || body.get("rejectReason") == null
                ? null
                : String.valueOf(body.get("rejectReason")).trim();
        if (!StringUtils.hasText(rejectReason)) {
            return Result.error("驳回原因不能为空");
        }
        if (rejectReason.length() > 255) {
            return Result.error("驳回原因不能超过 255 字");
        }

        LostFoundItem item = itemService.getById(id);
        if (item == null) {
            return Result.error("信息不存在");
        }
        if (item.getStatus() != null && item.getStatus() != 0) {
            return Result.error("仅待审核的信息可执行驳回操作");
        }

        boolean updated = itemService.updateStatus(id, 2, rejectReason);
        if (!updated) {
            return Result.error("审核驳回失败");
        }
        item.setStatus(2);
        item.setRejectReason(rejectReason);
        createReviewNotice(item, 2, rejectReason);
        return Result.success(item);
    }

    @PostMapping("/batch-action")
    public Result<Map<String, Object>> batchAction(@RequestBody Map<String, Object> body,
                                                   HttpServletRequest request) {
        Result<User> authResult = validateAdmin(request);
        if (authResult.getCode() != 200) {
            return Result.error(authResult.getMsg());
        }
        if (body == null || body.get("itemIds") == null || body.get("action") == null) {
            return Result.error("参数不完整");
        }

        String action = String.valueOf(body.get("action")).trim();
        String rejectReason = body.get("rejectReason") == null ? null : String.valueOf(body.get("rejectReason")).trim();
        if (!"approve".equals(action) && !"reject".equals(action)) {
            return Result.error("action 仅支持 approve / reject");
        }
        if ("reject".equals(action) && !StringUtils.hasText(rejectReason)) {
            return Result.error("批量驳回时 rejectReason 不能为空");
        }

        if (!(body.get("itemIds") instanceof List<?> rawIds)) {
            return Result.error("itemIds 格式错误");
        }
        Set<Long> itemIds = new HashSet<>();
        for (Object rawId : rawIds) {
            if (rawId instanceof Number) {
                itemIds.add(((Number) rawId).longValue());
            } else if (rawId != null) {
                try {
                    itemIds.add(Long.parseLong(String.valueOf(rawId)));
                } catch (NumberFormatException ignored) {
                    // ignore invalid id
                }
            }
        }
        if (itemIds.isEmpty()) {
            return Result.error("itemIds 不能为空");
        }

        List<Long> successIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();
        for (Long itemId : itemIds) {
            LostFoundItem item = itemService.getById(itemId);
            if (item == null || (item.getStatus() != null && item.getStatus() != 0)) {
                failedIds.add(itemId);
                continue;
            }

            boolean updated;
            if ("approve".equals(action)) {
                updated = itemService.updateStatus(itemId, 1, null);
                if (updated) {
                    createReviewNotice(item, 1, null);
                }
            } else {
                updated = itemService.updateStatus(itemId, 2, rejectReason);
                if (updated) {
                    createReviewNotice(item, 2, rejectReason);
                }
            }

            if (updated) {
                successIds.add(itemId);
            } else {
                failedIds.add(itemId);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("action", action);
        data.put("successCount", successIds.size());
        data.put("failedCount", failedIds.size());
        data.put("successIds", successIds);
        data.put("failedIds", failedIds);
        return Result.success(data);
    }

    /**
     * 全部信息管理 - 支持搜索、分类、状态、时间筛选和分页
     */
    @GetMapping("/items")
    public Result<Map<String, Object>> getAllItems(
            HttpServletRequest request,
            Integer page,
            Integer size,
            String keyword,
            String category,
            Integer status,
            String startDate,
            String endDate) {
        Result<User> authResult = validateAdmin(request);
        if (authResult.getCode() != 200) {
            return Result.error(authResult.getMsg());
        }

        if (page == null || page < 1) page = 1;
        if (size == null || size < 1 || size > 100) size = 20;

        long offset = (long) (page - 1) * size;

        var query = itemService.lambdaQuery();

        // 关键词搜索（物品名称、地点、描述）
        if (StringUtils.hasText(keyword)) {
            String searchKeyword = "%" + keyword.trim() + "%";
            query = query.and(q -> q
                    .like(LostFoundItem::getTitle, searchKeyword)
                    .or()
                    .like(LostFoundItem::getLocation, searchKeyword)
                    .or()
                    .like(LostFoundItem::getDescription, searchKeyword));
        }

        // 分类过滤
        if (StringUtils.hasText(category)) {
            query = query.eq(LostFoundItem::getCategory, category.trim());
        }

        // 状态过滤
        if (status != null) {
            query = query.eq(LostFoundItem::getStatus, status);
        }

        // 时间范围过滤
        if (StringUtils.hasText(startDate)) {
            try {
                LocalDateTime startDt = LocalDateTime.parse(startDate.trim().replace(" ", "T"));
                query = query.ge(LostFoundItem::getCreateTime, startDt);
            } catch (Exception ignored) {
                // 忽略日期解析错误
            }
        }
        if (StringUtils.hasText(endDate)) {
            try {
                LocalDateTime endDt = LocalDateTime.parse(endDate.trim().replace(" ", "T"));
                query = query.le(LostFoundItem::getCreateTime, endDt);
            } catch (Exception ignored) {
                // 忽略日期解析错误
            }
        }

        long total = query.count();
        List<LostFoundItem> items = query
                .orderByDesc(LostFoundItem::getCreateTime)
                .last("LIMIT " + size + " OFFSET " + offset)
                .list();

        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        data.put("pages", (total + size - 1) / size);
        return Result.success(data);
    }

    /**
     * 获取平台统计数据
     * 修复: 仅管理员可以访问统计接口
     */
    @GetMapping("/stats")
    public Result<Map<String, Long>> stats(HttpServletRequest request) {
        Result<User> authResult = validateAdmin(request);
        if (authResult.getCode() != 200) {
            return Result.error(authResult.getMsg());
        }

        long totalItems = itemService.count();
        long pendingReview = itemService.lambdaQuery().eq(LostFoundItem::getStatus, 0).count();
        long approved = itemService.lambdaQuery().eq(LostFoundItem::getStatus, 1).count();
        long rejected = itemService.lambdaQuery().eq(LostFoundItem::getStatus, 2).count();
        long totalUsers = userService.count();

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime nextDayStart = dayStart.plusDays(1);
        long todayPublished = itemService.lambdaQuery()
                .ge(LostFoundItem::getCreateTime, dayStart)
                .lt(LostFoundItem::getCreateTime, nextDayStart)
                .count();

        Map<String, Long> data = new HashMap<>();
        data.put("totalItems", totalItems);
        data.put("pendingReview", pendingReview);
        data.put("approved", approved);
        data.put("rejected", rejected);
        data.put("totalUsers", totalUsers);
        data.put("todayPublished", todayPublished);
        return Result.success(data);
    }

    /**
     * 获取用户列表 - 支持搜索、状态筛选和分页
     */
    @GetMapping("/users")
    public Result<Map<String, Object>> listUsers(
            HttpServletRequest request,
            Integer page,
            Integer size,
            String keyword,
            Integer status) {
        Result<User> authResult = validateAdmin(request);
        if (authResult.getCode() != 200) {
            return Result.error(authResult.getMsg());
        }

        if (page == null || page < 1) page = 1;
        if (size == null || size < 1 || size > 100) size = 20;

        long offset = (long) (page - 1) * size;

        var query = userService.lambdaQuery();

        // 关键词搜索（用户名、学号）
        if (StringUtils.hasText(keyword)) {
            String searchKeyword = "%" + keyword.trim() + "%";
            query = query.and(q -> q
                    .like(User::getUsername, searchKeyword)
                    .or()
                    .like(User::getStudentId, searchKeyword));
        }

        // 状态过滤（1正常 0封禁）
        if (status != null) {
            query = query.eq(User::getStatus, status);
        }

        long total = query.count();
        List<User> users = query
                .orderByDesc(User::getCreateTime)
                .last("LIMIT " + size + " OFFSET " + offset)
                .list();
        
        // 去除密码字段
        for (User u : users) {
            u.setPassword(null);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("users", users);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        data.put("pages", (total + size - 1) / size);
        return Result.success(data);
    }

    /**
     * 禁用用户
     */
    @PatchMapping("/users/{id}/ban")
    public Result<Void> banUser(@PathVariable Integer id, HttpServletRequest request) {
        Result<User> authResult = validateAdmin(request);
        if (authResult.getCode() != 200) {
            return Result.error(authResult.getMsg());
        }

        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        user.setStatus(0); // 0表示封禁
        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);
        return Result.success();
    }

    /**
     * 解除封禁
     */
    @PatchMapping("/users/{id}/unban")
    public Result<Void> unbanUser(@PathVariable Integer id, HttpServletRequest request) {
        Result<User> authResult = validateAdmin(request);
        if (authResult.getCode() != 200) {
            return Result.error(authResult.getMsg());
        }

        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        user.setStatus(1); // 1表示正常
        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);
        return Result.success();
    }
}