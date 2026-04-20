package com.campuslostfound.controller;

import com.campuslostfound.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FileController {

    @Value("${file.upload-path}")
    private String uploadPath;

    // 定义允许上传的后缀白名单 (根据业务需求调整)
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");

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

    /**
     * 上传文件接口
     * 修复: 添加用户身份验证，记录上传者信息
     */
    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file,
                                     HttpServletRequest request) {

        // ✅ 验证用户身份
        Integer userId = getAuthenticatedUserId(request);
        if (userId == null) {
            return Result.error("请先登录后上传文件");
        }

        // 判空
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        try {
            // --- 第一步：安全校验 ---
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                return Result.error("文件必须包含扩展名");
            }

            // 提取后缀并转小写
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();

            // 检查白名单
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                return Result.error("不支持的文件格式，仅允许：" + String.join(", ", ALLOWED_EXTENSIONS));
            }
            
            // 文件大小检查（5MB 限制）
            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.error("文件大小不能超过 5MB");
            }

            // --- 第二步：路径处理 ---
            LocalDate date = LocalDate.now();
            String dateFolder = String.format("%04d/%02d/%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
            
            // 规范化上传根路径
            String normalizedUploadPath = uploadPath;
            if (!normalizedUploadPath.endsWith("/") && !normalizedUploadPath.endsWith("\\")) {
                normalizedUploadPath += "/";
            }

            Path folderPath = Paths.get(normalizedUploadPath, dateFolder);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }

            // --- 第三步：生成唯一文件名 ---
            // 使用 UUID + 原始后缀，防止文件名冲突
            // 同时在文件名中包含userId用于审计，格式: {userId}_{UUID}{extension}
            String filename = userId + "_" + UUID.randomUUID().toString() + extension;
            Path targetPath = folderPath.resolve(filename);

            // --- 第四步：保存文件 ---
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // --- 第五步：构建访问 URL ---
            String baseUrl = request.getScheme() + "://" + request.getServerName();
            if (request.getServerPort() != 80 && request.getServerPort() != 443) {
                baseUrl += ":" + request.getServerPort();
            }
            // 注意：这里的 /uploads/ 必须和你 WebMvcConfigurer 配置的静态资源映射路径一致
            String fileUrl = baseUrl + "/uploads/" + dateFolder + "/" + filename;

            return Result.success(fileUrl);

        } catch (IOException e) {
            // 生产环境中建议记录详细日志 e.printStackTrace() 或使用 log.error
            return Result.error("文件上传失败，服务器内部错误：" + e.getMessage());
        }
    }
}
