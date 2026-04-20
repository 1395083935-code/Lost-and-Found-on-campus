package com.campuslostfound.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 数据加密工具类
 * 用于加密敏感信息（如电话号码、邮箱等）
 */
public class EncryptionUtils {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    // 加密密钥（32字节的密钥，对应256位）
    private static final String SECRET_KEY = "CampusLostFound1234567890123456";

    /**
     * 加密字符串
     * @param plaintext 明文
     * @return 加密后的Base64字符串
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            SecretKeySpec secretKey = new SecretKeySpec(
                    SECRET_KEY.getBytes(StandardCharsets.UTF_8), 0, 32, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密字符串
     * @param ciphertext 加密后的Base64字符串
     * @return 解密后的明文
     */
    public static String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        try {
            SecretKeySpec secretKey = new SecretKeySpec(
                    SECRET_KEY.getBytes(StandardCharsets.UTF_8), 0, 32, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(ciphertext);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 脱敏电话号码
     * 示例：13800138888 -> 138****8888
     * @param phoneNumber 完整电话号码
     * @return 脱敏后的电话号码
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return phoneNumber;
        }
        int len = phoneNumber.length();
        int maskStart = 3;
        int maskEnd = Math.max(3, len - 4);
        StringBuilder masked = new StringBuilder(phoneNumber);
        for (int i = maskStart; i < maskEnd; i++) {
            masked.setCharAt(i, '*');
        }
        return masked.toString();
    }

    /**
     * 检查是否是有效的加密格式（Base64）
     * @param text 待检查的文本
     * @return 是否为有效的Base64字符串
     */
    public static boolean isValidBase64(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        try {
            Base64.getDecoder().decode(text);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
