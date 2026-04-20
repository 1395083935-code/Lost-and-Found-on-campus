package com.campuslostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("lost_found_item")
public class LostFoundItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title; // 物品名称 2-20字
    
    private String description; // 物品描述 0-500字
    
    private Integer type; // 0失物 1招领
    
    private String category; // 分类：证件/电子/文具/衣物/其他
    
    private String location; // 丢失/拾获地点
    
    private String storageLocation; // 存放地点（仅拾物）
    
    @TableField("is_anonymous")
    private Integer anonymous; // 是否匿名：1是 0否
    
    private String imageUrl; // 图片URL（多个用逗号分隔）
    
    private String contactInfo; // 联系方式
    
    private Integer userId; // 发布者用户ID
    
    private Integer status; // 0待审核 1已通过 2已驳回 3已完结
    
    private String rejectReason; // 驳回原因
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}