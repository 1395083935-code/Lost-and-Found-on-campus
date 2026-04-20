package com.campuslostfound.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 定时任务服务
 * 处理物品过期、消息清理、搜索记录清理等定时操作
 */
@Service
public class ScheduledTaskService {

    @Autowired
    private LostFoundItemService lostFoundItemService;

    @Autowired(required = false)
    private SearchService searchService;

    /**
     * 每天凌晨1点执行：处理已过期的物品（审核通过超过30天）
     * cron表达式：秒 分 时 日 月 周几
     * 0 0 1 * * * 表示每天1点0分0秒执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void expireOldItemsTask() {
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            System.out.println("[ScheduledTask] 开始执行物品过期处理任务，执行时间：" + now.format(formatter));
            
            lostFoundItemService.expireOldItems();
            
            System.out.println("[ScheduledTask] 物品过期处理任务完成");
        } catch (Exception e) {
            System.err.println("[ScheduledTask] 物品过期处理任务异常：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 每天凌晨2点执行：清理30天前的搜索记录
     * cron表达式：0 0 2 * * * 表示每天2点0分0秒执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldSearchRecordsTask() {
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            System.out.println("[ScheduledTask] 开始执行搜索记录清理任务，执行时间：" + now.format(formatter));
            
            if (searchService != null) {
                searchService.cleanupOldSearchRecords();
            }
            
            System.out.println("[ScheduledTask] 搜索记录清理任务完成");
        } catch (Exception e) {
            System.err.println("[ScheduledTask] 搜索记录清理任务异常：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 每小时执行一次：清理30天前的已完结物品的相关通知
     * 0 0 * * * * 表示每小时的0分0秒执行
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupOldNoticesTask() {
        try {
            System.out.println("[ScheduledTask] 开始执行通知清理任务");
            // TODO: 实现通知清理逻辑
            System.out.println("[ScheduledTask] 通知清理任务完成");
        } catch (Exception e) {
            System.err.println("[ScheduledTask] 通知清理任务异常：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
