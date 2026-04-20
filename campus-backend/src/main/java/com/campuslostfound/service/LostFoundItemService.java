package com.campuslostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campuslostfound.entity.LostFoundItem;

public interface LostFoundItemService extends IService<LostFoundItem> {

    /**
     * 保存失物招领信息
     */
    boolean saveItem(LostFoundItem item);

    /**
     * 分页查询列表，支持按类型/关键词/分类/时间范围筛选
     * @param days 天数：1/3/7/null(全部)
     */
    IPage<LostFoundItem> listItems(int page, int size, Integer type, String keyword, String category, Integer status, Integer days);

    /**
     * 查询指定用户的发布列表（个人中心）
     */
    java.util.List<LostFoundItem> listByUserId(Integer userId);

    /**
     * 查询待审核信息
     */
    java.util.List<LostFoundItem> listPendingItems();

    /**
     * 更新信息状态
     */
    boolean updateStatus(Long itemId, Integer status, String rejectReason);

    /**
     * 处理过期的已通过物品（审核通过时间 > 30天）
     * 定时任务：每天凌晨1点执行
     */
    void expireOldItems();
}