package com.campuslostfound.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT Token 工具类
 * 生成和验证 JWT Token
 */
public class JwtUtils {

    // JWT 签名密钥（至少256位）
    private static final String SECRET_KEY = "campus_lost_found_system_secret_key_2024_12345678";
    private static final long TOKEN_EXPIRATION = 2 * 60 * 60 * 1000; // 2 小时
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000; // 7 天

    private static final SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    /**
     * 生成 JWT Token
     * @param userId 用户ID
     * @return JWT Token字符串
     */
    public static String generateToken(Integer userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成刷新 Token
     * @param userId 用户ID
     * @return Refresh Token字符串
     */
    public static String generateRefreshToken(Integer userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .claim("type", "refresh")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证 Token 并获取用户ID
     * @param token JWT Token字符串
     * @return 用户ID，如果无效则返回null
     */
    public static Integer getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String subject = claims.getSubject();
            return Integer.parseInt(subject);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查 Token 是否有效
     * @param token JWT Token字符串
     * @return true 如果有效，false 如果无效
     */
    public static boolean isTokenValid(String token) {
        return getUserIdFromToken(token) != null;
    }

    /**
     * 检查 Token 是否过期
     * @param token JWT Token字符串
     * @return true 如果已过期，false 如果未过期
     */
    public static boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
