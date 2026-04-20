package com.campuslostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notice")
public class Notice {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private Integer userId; // 接收通知的用户ID
    
    private Long itemId; // 相关物品ID
    
    private String content; // 通知内容
    
    private Integer type; // 通知类型：1审核通过 2审核驳回 3匹配提醒 4举报处理
    
    private Integer isRead; // 是否已读：1已读 0未读
    
    private LocalDateTime createTime;
}
