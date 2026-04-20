package com.campuslostfound.config;

import com.campuslostfound.common.Result;
import com.campuslostfound.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT Token 验证拦截器
 * 对需要认证的接口进行 token 校验
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        
        // 白名单检查
        if (isWhitelisted(request)) {
            return true;
        }

        // 需要认证的接口：检查 token
        String token = extractToken(request);
        if (!StringUtils.hasText(token) || !JwtUtils.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                Result.error("未登录或登录已过期，请重新登录")
            ));
            return false;
        }

        // token 有效，将 userId 放入 request attribute，供业务代码获取
        Integer userId = JwtUtils.getUserIdFromToken(token);
        if (userId != null) {
            request.setAttribute("userId", userId);
        }

        return true;
    }

    /**
     * 检查路径是否在白名单中（无需认证）
     */
    private boolean isWhitelisted(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 登录注册接口无需认证
        if (path.startsWith("/api/auth/")) {
            return true;
        }

        // 管理员登录接口无需认证
        if ("/api/admin/login".equals(path)) {
            return true;
        }

        // 公开查询接口无需认证（仅GET方法）
        if ("GET".equals(method) && isPublicGetPath(path)) {
            return true;
        }

        // 静态资源无需认证
        if (path.equals("/") || path.equals("/index.html") ||
            path.startsWith("/static/") || path.startsWith("/uploads/")) {
            return true;
        }

        return false;
    }

    private boolean isPublicGetPath(String path) {
        // ✅ 修复：删除/api/upload和/api/admin/stats，因为它们现在需要认证
        // /api/upload需要认证来记录上传者
        // /api/admin/stats需要管理员权限
        return "/api/items".equals(path)
                || path.matches("/api/items/\\d+")
                || path.startsWith("/api/search/");
    }

    /**
     * 从请求头提取 token
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
