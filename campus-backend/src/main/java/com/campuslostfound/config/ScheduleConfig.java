package com.campuslostfound.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置类
 * 支持物品自动过期处理、消息清理等定时任务
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
    // 定时任务配置已在main方法启用 @EnableScheduling
}
