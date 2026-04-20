package com.campuslostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("search_hotwords")
public class SearchHotwords {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private String keyword; // 搜索关键词
    
    private Integer searchCount; // 搜索次数
    
    private LocalDateTime lastSearchTime; // 最后搜索时间
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
