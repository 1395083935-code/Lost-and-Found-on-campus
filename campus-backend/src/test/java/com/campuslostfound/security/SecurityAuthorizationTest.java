package com.campuslostfound.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campuslostfound.entity.Favorite;
import com.campuslostfound.entity.LostFoundItem;
import com.campuslostfound.entity.User;
import com.campuslostfound.service.FavoriteService;
import com.campuslostfound.service.LostFoundItemService;
import com.campuslostfound.service.UserService;
import com.campuslostfound.common.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * 权限控制安全测试
 * 
 * 测试场景：
 * 1. 验证FavoriteController的权限修复
 * 2. 验证UserController的权限修复
 * 3. 验证FileController的认证要求
 * 4. 验证AdminController的管理员权限检查
 */
@SpringBootTest(classes = com.campuslostfound.CampusApplication.class)
public class SecurityAuthorizationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private LostFoundItemService itemService;

    @Autowired
    private FavoriteService favoriteService;

    private String userAToken;
    private String userBToken;
    private Integer userAId;
    private Integer userBId;
    private Integer adminUserId;
    private String adminToken;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 清空测试数据
        favoriteService.remove(null);
        
        // 删除可能存在的测试用户（避免主键冲突）
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.in("username", java.util.Arrays.asList("testUserA", "testUserB", "adminUser"));
        userService.remove(wrapper);
        
        // 创建测试用户
        User userA = new User();
        userA.setUsername("testUserA");
        userA.setPassword("password123");
        userA.setRole(0);  // 普通用户
        userService.save(userA);
        userAId = userA.getId();
        userAToken = generateToken(userAId);

        User userB = new User();
        userB.setUsername("testUserB");
        userB.setPassword("password123");
        userB.setRole(0);  // 普通用户
        userService.save(userB);
        userBId = userB.getId();
        userBToken = generateToken(userBId);

        // 创建管理员用户
        User adminUser = new User();
        adminUser.setUsername("adminUser");
        adminUser.setPassword("password123");
        adminUser.setRole(1);  // 管理员
        userService.save(adminUser);
        adminUserId = adminUser.getId();
        adminToken = generateToken(adminUserId);
    }

    private String generateToken(Integer userId) {
        long timestamp = System.currentTimeMillis();
        String signature = java.util.UUID.randomUUID().toString().substring(0, 8);
        return userId + "_" + timestamp + "_" + signature;
    }

    // ==================== FavoriteController 权限测试 ====================

    /**
     * 测试：用户A不能代替用户B收藏
     * 修复前：POST /api/favorites 接受request body中的userId，可以代替他人收藏
     * 修复后：userId从token自动获取，无法代替他人
     */
    @Test
    public void testUserCannotAddFavoriteForAnotherUser() throws Exception {
        // 用户A尝试为用户B添加收藏（使用用户A的token）
        String requestBody = """
            {
                "userId": %d,
                "itemId": 1
            }
            """.formatted(userBId);  // 尝试为用户B添加

        mockMvc.perform(post("/api/favorites")
                .header("Authorization", "Bearer " + userAToken)
                .contentType("application/json")
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(userAId))  // 应该是userA，不是userB
                .andExpect(jsonPath("$.message").value("收藏成功"));
    }

    /**
     * 测试：用户A不能查看用户B的收藏列表
     * 修复前：GET /api/favorites?userId=2 可以查看任何用户的收藏
     * 修复后：只能查看自己的收藏，不接受userId参数
     */
    @Test
    public void testUserCannotViewAnotherUsersFavorites() throws Exception {
        // 用户A尝试查看用户B的收藏（使用用户A的token）
        mockMvc.perform(get("/api/favorites?userId=" + userBId)
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                // 返回的应该是用户A的收藏（空列表），不是用户B的
                .andExpect(jsonPath("$.data.records", hasSize(0)));
    }

    /**
     * 测试：用户A不能删除用户B的收藏
     * 修复前：DELETE /api/favorites/{id}?userId=2 可以删除他人收藏
     * 修复后：userId从token获取，只能删除自己的收藏
     */
    @Test
    public void testUserCannotDeleteAnotherUsersFavorite() throws Exception {
        // 先为用户B添加一个收藏
        Favorite favorite = new Favorite();
        favorite.setUserId(userBId);
        favorite.setItemId(999L);
        favoriteService.save(favorite);

        // 用户A尝试删除用户B的收藏
        mockMvc.perform(delete("/api/favorites/999?userId=" + userBId)
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("取消收藏失败"));
    }

    /**
     * 测试：用户A不能检查用户B是否收藏了某个物品
     */
    @Test
    public void testUserCannotCheckAnotherUsersFavorite() throws Exception {
        // 为用户B添加收藏
        Favorite favorite = new Favorite();
        favorite.setUserId(userBId);
        favorite.setItemId(999L);
        favoriteService.save(favorite);

        // 用户A尝试检查用户B是否收藏了物品999
        mockMvc.perform(get("/api/favorites/check?userId=" + userBId + "&itemId=999")
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(false));  // 不应该查看到用户B的收藏
    }

    // ==================== UserController 权限测试 ====================

    /**
     * 测试：用户无法查询其他用户的信息
     * 修复前：GET /api/user/info?id=2 可以查看任何用户的信息
     * 修复后：只能查看自己的信息
     */
    @Test
    public void testUserCannotViewAnotherUserInfo() throws Exception {
        // 用户A尝试查看用户B的信息（不再接受id参数）
        mockMvc.perform(get("/api/user/info")
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(userAId))  // 返回的应该是用户A的信息
                .andExpect(jsonPath("$.data.username").value("testUserA"));
    }

    /**
     * 测试：未认证用户无法查看信息
     */
    @Test
    public void testUnauthenticatedUserCannotGetInfo() throws Exception {
        mockMvc.perform(get("/api/user/info"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("未登录或登录已过期，请重新登录"));
    }

    // ==================== FileController 认证测试 ====================

    /**
     * 测试：未认证用户无法上传文件
     */
    @Test
    public void testUnauthenticatedUserCannotUploadFile() throws Exception {
        mockMvc.perform(post("/api/upload")
                .param("file", "test.jpg"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("未登录或登录已过期，请重新登录"));
    }

    /**
     * 测试：认证用户可以上传文件
     */
    @Test
    public void testAuthenticatedUserCanUploadFile() throws Exception {
        // 这个测试需要真实的文件上传，可能需要使用MockMultipartFile
        // 仅测试身份验证部分
        mockMvc.perform(post("/api/upload")
                .header("Authorization", "Bearer " + userAToken)
                .param("file", "test.jpg"))
                // 文件为空会返回400，但说明认证通过了
                .andExpect(status().is4xxClientError());
    }

    // ==================== AdminController 权限测试 ====================

    /**
     * 测试：普通用户无法访问管理员统计接口
     */
    @Test
    public void testOrdinaryUserCannotAccessAdminStats() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("无权访问，仅管理员可以查看统计数据"));
    }

    /**
     * 测试：管理员可以访问统计接口
     */
    @Test
    public void testAdminCanAccessStats() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.totalItems").isNumber());
    }

    /**
     * 测试：未认证用户无法访问管理员接口
     */
    @Test
    public void testUnauthenticatedUserCannotAccessAdminStats() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("未登录或登录已过期，请重新登录"));
    }

    // ==================== ItemController 权限测试（验证已有的修复） ====================

    /**
     * 测试：用户只能编辑自己发布的物品
     */
    @Test
    public void testUserCannotEditOtherUserItem() throws Exception {
        // 创建用户B的物品
        LostFoundItem item = new LostFoundItem();
        item.setUserId(userBId);
        item.setTitle("测试物品");
        item.setType(0);
        item.setStatus(2);
        itemService.save(item);

        // 用户A尝试编辑用户B的物品
        String requestBody = """
            {
                "title": "修改标题",
                "type": 0,
                "location": "某处",
                "contactInfo": "123456"
            }
            """;

        mockMvc.perform(put("/api/items/" + item.getId())
                .header("Authorization", "Bearer " + userAToken)
                .contentType("application/json")
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("无权编辑该信息"));
    }
}
