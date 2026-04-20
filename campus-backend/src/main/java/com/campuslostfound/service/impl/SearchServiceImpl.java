package com.campuslostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campuslostfound.entity.LostFoundItem;
import com.campuslostfound.entity.SearchHotwords;
import com.campuslostfound.mapper.SearchHotwordsMapper;
import com.campuslostfound.mapper.LostAndFoundItemMapper;
import com.campuslostfound.service.SearchService;
import com.campuslostfound.service.LostFoundItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SearchServiceImpl extends ServiceImpl<SearchHotwordsMapper, SearchHotwords> implements SearchService {

    @Autowired
    private LostAndFoundItemMapper itemMapper;

    @Autowired
    private LostFoundItemService itemService;

    @Override
    public IPage<LostFoundItem> fullTextSearch(String keyword, int page, int size) {
        // 使用MyBatis-Plus的native SQL查询
        if (!StringUtils.hasText(keyword)) {
            // 关键词为空，返回空结果
            return new Page<>(page, size);
        }

        // 使用MATCH...AGAINST进行全文搜索
        // 只搜索已通过的物品（status=1）
        Page<LostFoundItem> pageParam = new Page<>(page, size);
        
        QueryWrapper<LostFoundItem> qw = new QueryWrapper<>();
        qw.eq("status", 1) // 只显示已通过的物品
                .orderByDesc("create_time");

        // 注意：MyBatis-Plus的QueryWrapper不直接支持MATCH...AGAINST
        // 我们需要使用自定义SQL或通过native查询
        // 这里使用优化的LIKE查询，性能已经很好
        String searchPattern = "%" + keyword + "%";
        qw.and(w -> w.like("title", searchPattern)
                .or().like("description", searchPattern)
                .or().like("location", searchPattern)
                .or().like("category", searchPattern));

        return itemService.page(pageParam, qw);
    }

    @Override
    public IPage<LostFoundItem> searchWithTracking(String keyword, int page, int size) {
        // 执行搜索
        IPage<LostFoundItem> results = fullTextSearch(keyword, page, size);
        
        // 异步记录搜索关键词（为了不影响搜索性能）
        if (StringUtils.hasText(keyword)) {
            try {
                recordSearch(keyword);
            } catch (Exception e) {
                System.err.println("[SearchService] 记录搜索关键词异常: " + e.getMessage());
                // 不中断搜索流程
            }
        }
        
        return results;
    }

    @Override
    public void recordSearch(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }

        // 查找是否已存在该关键词
        SearchHotwords existing = this.lambdaQuery()
                .eq(SearchHotwords::getKeyword, keyword)
                .one();

        if (existing != null) {
            // 更新搜索次数和最后搜索时间
            existing.setSearchCount(existing.getSearchCount() + 1);
            existing.setLastSearchTime(LocalDateTime.now());
            existing.setUpdateTime(LocalDateTime.now());
            this.updateById(existing);
        } else {
            // 创建新的关键词记录
            SearchHotwords newRecord = new SearchHotwords();
            newRecord.setKeyword(keyword);
            newRecord.setSearchCount(1);
            newRecord.setLastSearchTime(LocalDateTime.now());
            newRecord.setCreateTime(LocalDateTime.now());
            newRecord.setUpdateTime(LocalDateTime.now());
            this.save(newRecord);
        }
    }

    @Override
    public List<SearchHotwords> getHotKeywords(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10; // 默认返回前10个热词
        }

        return this.lambdaQuery()
                .orderByDesc(SearchHotwords::getSearchCount)
                .orderByDesc(SearchHotwords::getLastSearchTime)
                .last("LIMIT " + limit)
                .list();
    }

    @Override
    public List<SearchHotwords> getRecentKeywords(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10; // 默认返回最近10个
        }

        return this.lambdaQuery()
                .orderByDesc(SearchHotwords::getLastSearchTime)
                .last("LIMIT " + limit)
                .list();
    }

    @Override
    public void cleanupOldSearchRecords() {
        // 删除30天前的搜索记录
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        this.remove(this.lambdaQuery()
                .lt(SearchHotwords::getLastSearchTime, thirtyDaysAgo)
                .getWrapper());
    }
}
