package com.campuslostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private String username;
    
    private String password;
    
    private String nickname; // 用户昵称（微信昵称或用户设置的昵称）
    
    private String avatar;
    
    private String contactInfo;
    
    private String studentId;
    
    private Integer role; // 0普通用户 1管理员
    
    private Integer status; // 1正常 0封禁
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}