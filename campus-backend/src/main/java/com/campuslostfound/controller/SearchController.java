package com.campuslostfound.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campuslostfound.common.Result;
import com.campuslostfound.entity.LostFoundItem;
import com.campuslostfound.entity.SearchHotwords;
import com.campuslostfound.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    /**
     * 搜索物品（带热词统计）
     * 使用全文索引优化查询性能
     */
    @GetMapping("/items")
    public Result<Map<String, Object>> searchItems(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        if (!StringUtils.hasText(keyword)) {
            return Result.error("搜索关键词不能为空");
        }

        if (keyword.length() > 100) {
            return Result.error("搜索关键词不能超过100字");
        }

        try {
            // 执行搜索并自动记录热词
            IPage<LostFoundItem> searchResults = searchService.searchWithTracking(keyword, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("data", searchResults.getRecords());
            response.put("total", searchResults.getTotal());
            response.put("page", page);
            response.put("size", size);
            response.put("keyword", keyword);
            response.put("resultCount", searchResults.getRecords().size());

            return Result.success(response);

        } catch (Exception e) {
            System.err.println("[SearchController] 搜索异常: " + e.getMessage());
            e.printStackTrace();
            return Result.error("搜索失败，请稍后重试");
        }
    }

    /**
     * 获取热搜关键词
     * 返回搜索次数最多的关键词
     */
    @GetMapping("/hotwords")
    public Result<List<SearchHotwords>> getHotKeywords(
            @RequestParam(defaultValue = "10") Integer limit) {

        if (limit <= 0) {
            limit = 10;
        }
        if (limit > 100) {
            limit = 100;
        }

        try {
            List<SearchHotwords> hotwords = searchService.getHotKeywords(limit);
            return Result.success(hotwords);
        } catch (Exception e) {
            System.err.println("[SearchController] 获取热词异常: " + e.getMessage());
            e.printStackTrace();
            return Result.error("获取热词失败");
        }
    }

    /**
     * 获取最近搜索
     * 返回最近搜索过的关键词
     */
    @GetMapping("/recent")
    public Result<List<SearchHotwords>> getRecentKeywords(
            @RequestParam(defaultValue = "10") Integer limit) {

        if (limit <= 0) {
            limit = 10;
        }
        if (limit > 100) {
            limit = 100;
        }

        try {
            List<SearchHotwords> recent = searchService.getRecentKeywords(limit);
            return Result.success(recent);
        } catch (Exception e) {
            System.err.println("[SearchController] 获取最近搜索异常: " + e.getMessage());
            e.printStackTrace();
            return Result.error("获取最近搜索失败");
        }
    }

    /**
     * 获取搜索统计信息
     * 返回某个关键词的搜索统计
     */
    @GetMapping("/stats/{keyword}")
    public Result<Map<String, Object>> getKeywordStats(
            @PathVariable String keyword) {

        if (!StringUtils.hasText(keyword)) {
            return Result.error("关键词不能为空");
        }

        try {
            SearchHotwords stats = searchService.lambdaQuery()
                    .eq(SearchHotwords::getKeyword, keyword)
                    .one();

            if (stats == null) {
                return Result.error("该关键词暂无搜索记录");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("keyword", stats.getKeyword());
            response.put("searchCount", stats.getSearchCount());
            response.put("lastSearchTime", stats.getLastSearchTime());
            response.put("createTime", stats.getCreateTime());

            return Result.success(response);

        } catch (Exception e) {
            System.err.println("[SearchController] 获取搜索统计异常: " + e.getMessage());
            e.printStackTrace();
            return Result.error("获取统计失败");
        }
    }

    /**
     * 清理过期搜索记录
     * 管理员接口：删除30天前的搜索记录
     */
    @PostMapping("/cleanup")
    public Result<Void> cleanupOldRecords() {
        try {
            searchService.cleanupOldSearchRecords();
            return Result.success();
        } catch (Exception e) {
            System.err.println("[SearchController] 清理搜索记录异常: " + e.getMessage());
            e.printStackTrace();
            return Result.error("清理失败");
        }
    }
}
