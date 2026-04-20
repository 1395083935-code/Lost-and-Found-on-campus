package com.campuslostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campuslostfound.entity.LostFoundItem;
import com.campuslostfound.entity.SearchHotwords;
import java.util.List;

/**
 * 搜索服务
 * 支持全文搜索和热词统计
 */
public interface SearchService extends IService<SearchHotwords> {

    /**
     * 全文搜索物品（使用MySQL全文索引）
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    IPage<LostFoundItem> fullTextSearch(String keyword, int page, int size);

    /**
     * 搜索并记录热词
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    IPage<LostFoundItem> searchWithTracking(String keyword, int page, int size);

    /**
     * 获取热搜关键词
     * @param limit 返回数量
     * @return 热词列表（按搜索次数排序）
     */
    List<SearchHotwords> getHotKeywords(Integer limit);

    /**
     * 获取最近搜索关键词
     * @param limit 返回数量
     * @return 最近搜索词列表（按搜索时间排序）
     */
    List<SearchHotwords> getRecentKeywords(Integer limit);

    /**
     * 记录搜索关键词（更新热度）
     * @param keyword 搜索关键词
     */
    void recordSearch(String keyword);

    /**
     * 清理过期的搜索记录（30天前的）
     */
    void cleanupOldSearchRecords();
}
