package com.campuslostfound.controller;

import com.campuslostfound.common.Result;
import com.campuslostfound.entity.Notice;
import com.campuslostfound.service.NoticeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    /**
     * 获取当前用户的所有通知
     */
    @GetMapping("/list")
    public Result<List<Notice>> getNotices(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        
        List<Notice> notices = noticeService.getUserNotices(userId);
        return Result.success(notices);
    }

    /**
     * 获取未读通知数
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> getUnreadCount(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        
        long count = noticeService.getUnreadCount(userId);
        Map<String, Long> response = new HashMap<>();
        response.put("unreadCount", count);
        return Result.success(response);
    }

    /**
     * 标记单个通知为已读
     */
    @PostMapping("/{noticeId}/mark-read")
    public Result<Void> markAsRead(
            @PathVariable Integer noticeId,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        
        noticeService.markAsRead(noticeId);
        return Result.success();
    }

    /**
     * 标记全部通知为已读
     */
    @PostMapping("/mark-all-read")
    public Result<Void> markAllAsRead(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        
        noticeService.markAllAsRead(userId);
        return Result.success();
    }

    /**
     * 删除单个通知
     */
    @DeleteMapping("/{noticeId}")
    public Result<Void> deleteNotice(
            @PathVariable Integer noticeId,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        
        noticeService.deleteNotice(noticeId);
        return Result.success();
    }

    /**
     * 删除全部通知
     */
    @DeleteMapping("/delete-all")
    public Result<Void> deleteAllNotices(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        
        // 删除该用户的所有通知
        List<Notice> notices = noticeService.getUserNotices(userId);
        for (Notice notice : notices) {
            noticeService.deleteNotice(notice.getId());
        }
        
        return Result.success();
    }
}
