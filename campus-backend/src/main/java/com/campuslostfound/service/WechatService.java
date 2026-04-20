package com.campuslostfound.service;

import java.util.Map;

/**
 * 微信授权服务
 * 处理微信小程序的登录和授权
 */
public interface WechatService {

    /**
     * 通过微信授权码获取用户openid和session_key
     * @param code 微信授权码
     * @return 包含openid和session_key的Map
     */
    Map<String, String> getWechatUserInfo(String code);

    /**
     * 根据openid注册或更新用户信息
     * @param openid 微信openid
     * @param nickname 微信昵称
     * @param avatar 微信头像URL
     * @return 用户ID
     */
    Integer registerOrUpdateUser(String openid, String nickname, String avatar);

    /**
     * 通过openid获取用户ID
     * @param openid 微信openid
     * @return 用户ID，如果不存在返回null
     */
    Integer getUserIdByOpenid(String openid);

    /**
     * 验证签名（用于验证微信服务器消息）
     * @param timestamp 时间戳
     * @param nonce 随机字符串
     * @param signature 签名
     * @return true表示验证成功
     */
    boolean verifySignature(String timestamp, String nonce, String signature);
}
