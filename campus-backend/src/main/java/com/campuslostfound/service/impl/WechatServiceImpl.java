package com.campuslostfound.service.impl;

import com.campuslostfound.entity.User;
import com.campuslostfound.service.WechatService;
import com.campuslostfound.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.security.MessageDigest;
import java.util.*;

@Service
public class WechatServiceImpl implements WechatService {

    @Value("${wechat.appid:}")
    private String wechatAppId;

    @Value("${wechat.appsecret:}")
    private String wechatAppSecret;

    @Value("${wechat.token:}")
    private String wechatToken;

    @Autowired
    private UserService userService;

    @Autowired
    private RestTemplate restTemplate;

    private static final String WECHAT_AUTH_URL = "https://api.weixin.qq.com/sns/jscode2session";

    @Override
    public Map<String, String> getWechatUserInfo(String code) {
        try {
            // 调用微信服务器获取openid和session_key
            String url = String.format(
                    "%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    WECHAT_AUTH_URL, wechatAppId, wechatAppSecret, code);

            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSON.parseObject(response);

            if (json.containsKey("errcode")) {
                System.err.println("[WechatService] 获取微信用户信息失败：" + json.getString("errmsg"));
                return null;
            }

            Map<String, String> result = new HashMap<>();
            result.put("openid", json.getString("openid"));
            result.put("session_key", json.getString("session_key"));
            return result;

        } catch (Exception e) {
            System.err.println("[WechatService] 调用微信API异常：" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Integer registerOrUpdateUser(String openid, String nickname, String avatar) {
        try {
            // 查询该openid是否已注册
            User existingUser = userService.lambdaQuery()
                    .eq(User::getUsername, openid)
                    .one();

            if (existingUser != null) {
                // 更新头像和昵称
                existingUser.setNickname(nickname);
                existingUser.setAvatar(avatar);
                userService.updateById(existingUser);
                return existingUser.getId();
            }

            // 新建用户
            User newUser = new User();
            newUser.setUsername(openid);
            newUser.setPassword(""); // 微信登录不需要密码
            newUser.setNickname(nickname);
            newUser.setAvatar(avatar);
            newUser.setRole(0); // 普通用户
            newUser.setStatus(1); // 正常状态
            newUser.setContactInfo(""); // 初始为空，用户可后续填写

            userService.save(newUser);
            return newUser.getId();

        } catch (Exception e) {
            System.err.println("[WechatService] 注册或更新用户异常：" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Integer getUserIdByOpenid(String openid) {
        User user = userService.lambdaQuery()
                .eq(User::getUsername, openid)
                .one();
        return user != null ? user.getId() : null;
    }

    @Override
    public boolean verifySignature(String timestamp, String nonce, String signature) {
        try {
            // 将token、timestamp、nonce三个参数进行字典序排序
            String[] arr = {wechatToken, timestamp, nonce};
            Arrays.sort(arr);
            String str = arr[0] + arr[1] + arr[2];

            // SHA1加密
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(str.getBytes());
            String hexString = bytesToHex(digest);

            return hexString.equals(signature);
        } catch (Exception e) {
            System.err.println("[WechatService] 验证签名异常：" + e.getMessage());
            return false;
        }
    }

    /**
     * 字节数组转16进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() < 2) {
                sb.append(0);
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
