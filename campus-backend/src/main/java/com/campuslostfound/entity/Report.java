package com.campuslostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private Long itemId; // 被举报的物品ID
    
    private Integer reporterId; // 举报人用户ID
    
    private String reason; // 举报原因：虚假信息/垃圾广告/恶意骚扰
    
    private String description; // 详细说明
    
    private Integer status; // 0待处理 1已处理 2已驳回
    
    private Integer handlerId; // 处理人管理员ID
    
    private String handleReason; // 处理说明
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
