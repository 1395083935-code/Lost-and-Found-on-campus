package com.campuslostfound.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campuslostfound.entity.Notice;
import java.util.List;

public interface NoticeService extends IService<Notice> {
    
    /**
     * 创建通知
     */
    void createNotice(Integer userId, Long itemId, String content, Integer type);
    
    /**
     * 获取用户的所有通知
     */
    List<Notice> getUserNotices(Integer userId);
    
    /**
     * 获取未读通知数
     */
    long getUnreadCount(Integer userId);
    
    /**
     * 标记为已读
     */
    void markAsRead(Integer noticeId);
    
    /**
     * 标记全部已读
     */
    void markAllAsRead(Integer userId);
    
    /**
     * 删除通知
     */
    void deleteNotice(Integer noticeId);
}
