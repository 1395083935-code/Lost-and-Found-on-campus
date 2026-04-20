package com.campuslostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("admin")
public class Admin {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private String username; // 管理员账号
    
    private String password; // 密码（bcrypt加密）
    
    private Integer role; // 1超级管理员 2普通管理员
    
    private Integer status; // 1正常 0禁用
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
