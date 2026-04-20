package com.campuslostfound.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campuslostfound.common.Result;
import com.campuslostfound.entity.Report;
import com.campuslostfound.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

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
     * 举报物品信息
     */
    @PostMapping("/create")
    public Result<Void> createReport(
            @RequestParam Long itemId,
            @RequestParam String reason,
            @RequestParam(required = false) String description,
            HttpServletRequest request) {
        
        Integer reporterId = getAuthenticatedUserId(request);
        if (reporterId == null) {
            return Result.error("请先登录");
        }

        if (itemId == null || itemId <= 0) {
            return Result.error("物品ID无效");
        }

        if (!StringUtils.hasText(reason)) {
            return Result.error("举报原因不能为空");
        }

        // 验证举报原因是否在允许的范围内
        if (!isValidReason(reason)) {
            return Result.error("举报原因不合法");
        }

        boolean success = reportService.createReport(itemId, reporterId, reason, description);
        if (!success) {
            return Result.error("举报失败，请重试");
        }

        return Result.success();
    }

    /**
     * 查询待处理的举报列表（管理员）
     */
    @GetMapping("/pending")
    public Result<IPage<Report>> listPendingReports(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        
        // 仅管理员可访问
        if (!isAdmin(request)) {
            return Result.error("权限不足");
        }

        IPage<Report> pageData = reportService.listPendingReports(page, size);
        return Result.success(pageData);
    }

    /**
     * 查询所有举报记录（管理员）
     */
    @GetMapping("/list")
    public Result<IPage<Report>> listAllReports(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        
        // 仅管理员可访问
        if (!isAdmin(request)) {
            return Result.error("权限不足");
        }

        IPage<Report> pageData = reportService.listAllReports(page, size);
        return Result.success(pageData);
    }

    /**
     * 处理举报（管理员）
     * action: delete_item(删除物品), ban_user(封禁用户), dismiss(驳回)
     */
    @PostMapping("/handle")
    public Result<Void> handleReport(
            @RequestParam Integer reportId,
            @RequestParam String action,
            @RequestParam(required = false) String handleReason,
            HttpServletRequest request) {
        
        // 仅管理员可访问
        if (!isAdmin(request)) {
            return Result.error("权限不足");
        }

        if (reportId == null || reportId <= 0) {
            return Result.error("举报ID无效");
        }

        if (!StringUtils.hasText(action)) {
            return Result.error("处理方式不能为空");
        }

        if (!isValidAction(action)) {
            return Result.error("处理方式不合法");
        }

        boolean success = reportService.handleReport(reportId, action, handleReason);
        if (!success) {
            return Result.error("处理失败，该举报可能已处理");
        }

        return Result.success();
    }

    /**
     * 验证举报原因
     */
    private boolean isValidReason(String reason) {
        return reason.equals("虚假信息") || reason.equals("垃圾广告") || 
               reason.equals("恶意骚扰") || reason.equals("其他");
    }

    /**
     * 验证处理方式
     */
    private boolean isValidAction(String action) {
        return action.equals("delete_item") || action.equals("ban_user") || action.equals("dismiss");
    }

    /**
     * 检查是否为管理员
     */
    private boolean isAdmin(HttpServletRequest request) {
        Object roleObj = request.getAttribute("role");
        if (roleObj instanceof Integer) {
            return ((Integer) roleObj) == 1;
        }
        return false;
    }
}
