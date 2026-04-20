package com.campuslostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campuslostfound.entity.Report;
import com.campuslostfound.entity.User;
import com.campuslostfound.entity.LostFoundItem;
import com.campuslostfound.mapper.ReportMapper;
import com.campuslostfound.service.ReportService;
import com.campuslostfound.service.UserService;
import com.campuslostfound.service.LostFoundItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {

    @Autowired
    private UserService userService;

    @Autowired
    private LostFoundItemService lostFoundItemService;

    @Override
    public boolean createReport(Long itemId, Integer reporterId, String reason, String description) {
        Report report = new Report();
        report.setItemId(itemId);
        report.setReporterId(reporterId);
        report.setReason(reason);
        report.setDescription(description);
        report.setStatus(0); // 待处理
        report.setCreateTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());
        return this.save(report);
    }

    @Override
    public IPage<Report> listPendingReports(int page, int size) {
        Page<Report> pageParam = new Page<>(page, size);
        QueryWrapper<Report> qw = new QueryWrapper<>();
        qw.eq("status", 0) // 待处理
                .orderByAsc("create_time");
        return this.page(pageParam, qw);
    }

    @Override
    public IPage<Report> listAllReports(int page, int size) {
        Page<Report> pageParam = new Page<>(page, size);
        QueryWrapper<Report> qw = new QueryWrapper<>();
        qw.orderByDesc("create_time");
        return this.page(pageParam, qw);
    }

    @Override
    public boolean handleReport(Integer reportId, String action, String handleReason) {
        Report report = this.getById(reportId);
        if (report == null || report.getStatus() != 0) {
            return false;
        }

        // action: delete_item, ban_user, dismiss
        if ("delete_item".equals(action)) {
            // 删除物品
            lostFoundItemService.removeById(report.getItemId());
        } else if ("ban_user".equals(action)) {
            // 封禁用户：获取物品的发布者，封禁该用户
            LostFoundItem item = lostFoundItemService.getById(report.getItemId());
            if (item != null) {
                User user = userService.getById(item.getUserId());
                if (user != null) {
                    user.setStatus(0); // 0=封禁，1=正常
                    userService.updateById(user);
                }
            }
        }

        // 更新举报记录状态为已处理
        report.setStatus(1);
        report.setHandlerId(1); // 假设管理员ID为1，实际应从token获取
        report.setHandleReason(handleReason);
        report.setUpdateTime(LocalDateTime.now());
        return this.updateById(report);
    }

    @Override
    public int getReportCountByUserId(Integer userId) {
        // 获取该用户发布的物品被举报的总次数
        java.util.List<LostFoundItem> userItems = lostFoundItemService.lambdaQuery()
                .eq(LostFoundItem::getUserId, userId)
                .list();
        
        if (userItems.isEmpty()) {
            return 0;
        }

        java.util.List<Long> itemIds = userItems.stream()
                .map(LostFoundItem::getId)
                .collect(java.util.stream.Collectors.toList());

        long count = this.lambdaQuery()
                .in(Report::getItemId, itemIds)
                .count();
        
        return (int) count;
    }
}
