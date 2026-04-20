package com.campuslostfound.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campuslostfound.common.Result;
import com.campuslostfound.entity.Favorite;
import com.campuslostfound.entity.LostFoundItem;
import com.campuslostfound.service.FavoriteService;
import com.campuslostfound.service.LostFoundItemService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private LostFoundItemService itemService;

    /**
     * 从HttpServletRequest中获取已认证的用户ID
     * userId由JwtInterceptor设置到request attribute中
     */
    private Integer getAuthenticatedUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId instanceof Integer) {
            return (Integer) userId;
        }
        if (userId instanceof Number) {
            return ((Number) userId).intValue();
        }
        return null;
    }

    /**
     * 添加收藏
     * 修复: userId从认证上下文获取，不信任客户端参数
     */
    @PostMapping("/favorites")
    public Result<Favorite> addFavorite(@RequestBody Favorite favorite, HttpServletRequest request) {
        if (favorite == null || favorite.getItemId() == null) {
            return Result.error("参数不完整");
        }

        // ✅ 从认证上下文获取userId，不信任客户端
        Integer userId = getAuthenticatedUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        if (itemService.getById(favorite.getItemId()) == null) {
            return Result.error("收藏的物品不存在");
        }

        // 检查是否已收藏
        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("item_id", favorite.getItemId());

        Favorite existing = favoriteService.getOne(wrapper);
        if (existing != null) {
            return Result.error("已收藏该物品");
        }

        // 设置userId为当前认证用户
        favorite.setUserId(userId);
        boolean saved = favoriteService.save(favorite);
        if (saved) {
            return Result.success(favorite);
        } else {
            return Result.error("收藏失败");
        }
    }

    /**
     * 获取当前用户的收藏列表
     * 修复: userId从认证上下文获取，用户只能查看自己的收藏
     */
    @GetMapping("/favorites")
    public Result<Page<LostFoundItem>> getFavorites(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        // ✅ 从认证上下文获取userId，不接受客户端参数
        Integer userId = getAuthenticatedUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        // 获取当前用户收藏的物品ID列表
        QueryWrapper<Favorite> favoriteWrapper = new QueryWrapper<>();
        favoriteWrapper.eq("user_id", userId);
        List<Favorite> favorites = favoriteService.list(favoriteWrapper);

        if (favorites.isEmpty()) {
            return Result.success(new Page<>(current, size));
        }

        List<Long> itemIds = favorites.stream()
                .map(Favorite::getItemId)
                .collect(Collectors.toList());

        // 获取收藏的物品详情
        Page<LostFoundItem> page = new Page<>(current, size);
        QueryWrapper<LostFoundItem> itemWrapper = new QueryWrapper<>();
        itemWrapper.in("id", itemIds)
                   .orderByDesc("create_time");

        Page<LostFoundItem> result = itemService.page(page, itemWrapper);
        return Result.success(result);
    }

    /**
     * 取消收藏
     * 修复: userId从认证上下文获取，用户只能删除自己的收藏
     */
    @DeleteMapping("/favorites/{itemId}")
    public Result<Void> removeFavorite(@PathVariable Long itemId, HttpServletRequest request) {
        // ✅ 从认证上下文获取userId，不接受客户端参数
        Integer userId = getAuthenticatedUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("item_id", itemId);

        boolean removed = favoriteService.remove(wrapper);
        if (removed) {
            return Result.success(null);
        } else {
            return Result.error("取消收藏失败");
        }
    }

    /**
     * 检查是否已收藏
     * 修复: userId从认证上下文获取，用户只能检查自己的收藏
     */
    @GetMapping("/favorites/check")
    public Result<Boolean> checkFavorite(@RequestParam Long itemId, HttpServletRequest request) {
        // ✅ 从认证上下文获取userId，不接受客户端参数
        Integer userId = getAuthenticatedUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("item_id", itemId);

        Favorite favorite = favoriteService.getOne(wrapper);
        return Result.success(favorite != null);
    }
}