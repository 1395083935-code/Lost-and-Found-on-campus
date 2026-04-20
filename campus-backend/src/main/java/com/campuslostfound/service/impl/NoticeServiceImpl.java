package com.campuslostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campuslostfound.entity.Notice;
import com.campuslostfound.mapper.NoticeMapper;
import com.campuslostfound.service.NoticeService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    /**
     * 创建通知
     */
    @Override
    public void createNotice(Integer userId, Long itemId, String content, Integer type) {
        Notice notice = new Notice();
        notice.setUserId(userId);
        notice.setItemId(itemId);
        notice.setContent(content);
        notice.setType(type);
        notice.setIsRead(0); // 默认未读
        notice.setCreateTime(LocalDateTime.now());
        this.save(notice);
    }

    /**
     * 获取用户的所有通知（按时间倒序）
     */
    @Override
    public List<Notice> getUserNotices(Integer userId) {
        return this.list(
            new QueryWrapper<Notice>()
                .eq("user_id", userId)
                .orderByDesc("create_time")
        );
    }

    /**
     * 获取未读通知数
     */
    @Override
    public long getUnreadCount(Integer userId) {
        return this.count(
            new QueryWrapper<Notice>()
                .eq("user_id", userId)
                .eq("is_read", 0)
        );
    }

    /**
     * 标记为已读
     */
    @Override
    public void markAsRead(Integer noticeId) {
        Notice notice = new Notice();
        notice.setId(noticeId);
        notice.setIsRead(1);
        this.updateById(notice);
    }

    /**
     * 标记全部已读
     */
    @Override
    public void markAllAsRead(Integer userId) {
        Notice notice = new Notice();
        notice.setIsRead(1);
        this.update(notice, 
            new QueryWrapper<Notice>()
                .eq("user_id", userId)
                .eq("is_read", 0)
        );
    }

    /**
     * 删除通知
     */
    @Override
    public void deleteNotice(Integer noticeId) {
        this.removeById(noticeId);
    }
}
