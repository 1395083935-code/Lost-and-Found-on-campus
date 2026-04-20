package com.campuslostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campuslostfound.entity.LostFoundItem;
import com.campuslostfound.mapper.LostAndFoundItemMapper;
import com.campuslostfound.service.LostFoundItemService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LostAndFoundItemServiceImpl extends ServiceImpl<LostAndFoundItemMapper, LostFoundItem> implements LostFoundItemService {

    private static final java.util.Map<String, List<String>> CATEGORY_ALIASES = java.util.Map.of(
            "证件", List.of("证件", "certificate"),
            "电子产品", List.of("电子产品", "electronic"),
            "文具饰品", List.of("文具饰品", "stationery"),
            "衣物箱包", List.of("衣物箱包", "clothing"),
            "其他", List.of("其他", "other")
    );

    @Override
    public boolean saveItem(LostFoundItem item) {
        return this.save(item);
    }

    @Override
    public IPage<LostFoundItem> listItems(int page, int size, Integer type, String keyword, String category, Integer status, Integer days) {
        Page<LostFoundItem> pageParam = new Page<>(page, size);
        QueryWrapper<LostFoundItem> qw = new QueryWrapper<>();
        if (type != null) {
            qw.eq("type", type);
        }
        if (status != null) {
            qw.eq("status", status);
        } else {
            // 首页列表只显示已通过的物品（status=1），不显示待审核、驳回、过期
            qw.eq("status", 1);
        }
        if (StringUtils.hasText(category)) {
            List<String> categoryCandidates = new ArrayList<>(CATEGORY_ALIASES.getOrDefault(category, List.of(category)));
            qw.and(wrapper -> wrapper.in("category", categoryCandidates));
        }
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like("title", keyword)
                    .or().like("description", keyword)
                    .or().like("location", keyword));
        }
        // 时间范围筛选：days = 1/3/7 表示最近 N 天内的信息
        if (days != null && days > 0) {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            qw.ge("create_time", since);
        }
        qw.orderByDesc("create_time");
        return this.page(pageParam, qw);
    }

    @Override
    public List<LostFoundItem> listByUserId(Integer userId) {
        return this.lambdaQuery()
                .eq(LostFoundItem::getUserId, userId)
                .orderByDesc(LostFoundItem::getCreateTime)
                .list();
    }

    @Override
    public List<LostFoundItem> listPendingItems() {
        return this.lambdaQuery()
                .eq(LostFoundItem::getStatus, 0)
                .orderByAsc(LostFoundItem::getCreateTime)
                .list();
    }

    @Override
    public boolean updateStatus(Long itemId, Integer status, String rejectReason) {
        LostFoundItem item = this.getById(itemId);
        if (item == null) {
            return false;
        }
        item.setStatus(status);
        item.setRejectReason(rejectReason);
        item.setUpdateTime(LocalDateTime.now());
        return this.updateById(item);
    }

    @Override
    public void expireOldItems() {
        // 查询所有已通过的物品，审核通过已超过30天
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<LostFoundItem> expiredItems = this.lambdaQuery()
                .eq(LostFoundItem::getStatus, 1) // 已通过
                .lt(LostFoundItem::getCreateTime, thirtyDaysAgo)
                .list();
        
        // 标记为已过期（status=4）
        for (LostFoundItem item : expiredItems) {
            item.setStatus(4); // 4=已过期
            item.setUpdateTime(LocalDateTime.now());
            this.updateById(item);
        }
        
        if (!expiredItems.isEmpty()) {
            System.out.println("[ScheduledTask] 处理了 " + expiredItems.size() + " 个已过期的物品");
        }
    }
}