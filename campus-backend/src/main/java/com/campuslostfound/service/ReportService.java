package com.campuslostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campuslostfound.entity.Report;

public interface ReportService extends IService<Report> {

    /**
     * 创建举报记录
     */
    boolean createReport(Long itemId, Integer reporterId, String reason, String description);

    /**
     * 查询待处理的举报列表（分页）
     */
    IPage<Report> listPendingReports(int page, int size);

    /**
     * 查询所有举报记录（分页）
     */
    IPage<Report> listAllReports(int page, int size);

    /**
     * 处理举报：删除物品或封禁用户
     */
    boolean handleReport(Integer reportId, String action, String handleReason);

    /**
     * 查询用户的举报记录数量
     */
    int getReportCountByUserId(Integer userId);
}
