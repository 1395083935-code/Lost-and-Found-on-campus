package com.campuslostfound.controller;

import com.campuslostfound.common.Result;
import com.campuslostfound.entity.User;
import com.campuslostfound.service.UserService;
import com.campuslostfound.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * 从HttpServletRequest中获取已认证的用户ID
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

    @PostMapping("/auth/register")
    public Result<java.util.Map<String, Object>> register(@RequestBody User user) {
        if (!StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            return Result.error("用户名和密码不能为空");
        }
        // 用户名唯一性校验
        User existing = userService.lambdaQuery().eq(User::getUsername, user.getUsername()).one();
        if (existing != null) {
            return Result.error("用户名已存在");
        }
        // BCrypt 加密密码后保存
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole(0);
        }
        boolean saved = userService.save(user);
        if (saved) {
            user.setPassword(null); // 不返回密码字段
            String token = generateToken(user.getId());
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("user", user);
            result.put("token", token);
            return Result.success(result);
        } else {
            return Result.error("注册失败");
        }
    }

    @PostMapping("/auth/login")
    public Result<java.util.Map<String, Object>> login(@RequestBody User loginRequest) {
        if (!StringUtils.hasText(loginRequest.getUsername()) || !StringUtils.hasText(loginRequest.getPassword())) {
            return Result.error("用户名和密码不能为空");
        }
        User user = userService.lambdaQuery().eq(User::getUsername, loginRequest.getUsername()).one();
        // BCrypt 验证（已加密的密码才能通过）
        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return Result.error("用户名或密码错误");
        }
        user.setPassword(null); // 不返回密码字段
        String token = generateToken(user.getId());
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("user", user);
        result.put("token", token);
        return Result.success(result);
    }

    /**
     * 生成 JWT Token
     * 使用 JwtUtils 生成标准的 JWT Token
     */
    private String generateToken(Integer userId) {
        return JwtUtils.generateToken(userId);
    }

    /**
     * 获取当前登录用户的信息
     * 修复: 不再接受id参数，只返回当前认证用户的信息
     */
    @GetMapping("/user/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        // ✅ 从认证上下文获取userId，不接受客户端参数
        Integer userId = getAuthenticatedUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在或登录已失效");
        }
        user.setPassword(null); // 不返回密码字段
        return Result.success(user);
    }

    /**
     * 登出
     * 修复: 删除不必要的id参数
     */
    @PostMapping("/auth/logout")
    public Result<Boolean> logout() {
        // 当前版本为无状态登录，服务端无需维护会话，直接返回成功。
        // 客户端需要删除本地存储的token
        return Result.success(true);
    }
}