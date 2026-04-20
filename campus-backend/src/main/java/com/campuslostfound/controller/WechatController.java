package com.campuslostfound.controller;

import com.campuslostfound.common.Result;
import com.campuslostfound.service.WechatService;
import com.campuslostfound.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/wechat")
public class WechatController {

    @Autowired
    private WechatService wechatService;

    /**
     * 微信登录接口
     * 前端通过wx.login()获取code，然后调用此接口
     * @param code 微信授权码
     * @param nickname 微信昵称（可选，如果前端获取到则传递）
     * @param avatar 微信头像URL（可选）
     * @return JWT Token和用户信息
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> wechatLogin(
            @RequestParam String code,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String avatar) {

        if (!StringUtils.hasText(code)) {
            return Result.error("授权码不能为空");
        }

        try {
            // 1. 通过code获取微信用户的openid和session_key
            Map<String, String> wechatInfo = wechatService.getWechatUserInfo(code);
            if (wechatInfo == null || !wechatInfo.containsKey("openid")) {
                return Result.error("微信授权失败，请重试");
            }

            String openid = wechatInfo.get("openid");
            String sessionKey = wechatInfo.get("session_key");

            // 2. 使用默认昵称（如果前端没有传递）
            if (!StringUtils.hasText(nickname)) {
                nickname = "User_" + openid.substring(0, 8);
            }

            // 3. 注册或更新用户
            Integer userId = wechatService.registerOrUpdateUser(openid, nickname, avatar != null ? avatar : "");
            if (userId == null) {
                return Result.error("用户注册失败");
            }

            // 4. 生成JWT Token
            String token = JwtUtils.generateToken(userId);
            String refreshToken = JwtUtils.generateRefreshToken(userId);

            // 5. 返回token和用户信息
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("refreshToken", refreshToken);
            response.put("userId", userId);
            response.put("nickname", nickname);
            response.put("avatar", avatar);
            response.put("openid", openid);

            return Result.success(response);

        } catch (Exception e) {
            System.err.println("[WechatController] 微信登录异常：" + e.getMessage());
            e.printStackTrace();
            return Result.error("登录失败，请稍后重试");
        }
    }

    /**
     * 刷新Token接口
     * @param refreshToken 刷新令牌
     * @return 新的JWT Token
     */
    @PostMapping("/refresh-token")
    public Result<Map<String, String>> refreshToken(
            @RequestParam String refreshToken) {

        if (!StringUtils.hasText(refreshToken)) {
            return Result.error("刷新令牌不能为空");
        }

        try {
            // 验证refreshToken是否有效
            if (!JwtUtils.isTokenValid(refreshToken)) {
                return Result.error("刷新令牌已过期，请重新登录");
            }

            // 从refreshToken中获取userId
            Integer userId = JwtUtils.getUserIdFromToken(refreshToken);
            if (userId == null) {
                return Result.error("刷新令牌无效");
            }

            // 生成新的token
            String newToken = JwtUtils.generateToken(userId);

            Map<String, String> response = new HashMap<>();
            response.put("token", newToken);
            response.put("refreshToken", refreshToken);

            return Result.success(response);

        } catch (Exception e) {
            System.err.println("[WechatController] Token刷新异常：" + e.getMessage());
            e.printStackTrace();
            return Result.error("Token刷新失败");
        }
    }

    /**
     * 验证Token有效性
     * @param token JWT Token
     * @return true表示有效，false表示无效
     */
    @GetMapping("/verify-token")
    public Result<Map<String, Boolean>> verifyToken(
            @RequestParam String token) {

        boolean isValid = JwtUtils.isTokenValid(token);
        boolean isExpired = JwtUtils.isTokenExpired(token);

        Map<String, Boolean> response = new HashMap<>();
        response.put("valid", isValid);
        response.put("expired", isExpired);

        return Result.success(response);
    }
}
