package com.shen.security.util;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTHeader;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.RegisteredPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 * 基于 Hutool JWT 实现，负责 token 的生成、验证和解析
 */
@Component
public class JwtTokenUtil {

    @Value("${jwt.secret:your-secret-key-here}")
    private String secret;

    @Value("${jwt.expiration:7200}")
    private Long expiration;

    /**
     * 生成 token
     *
     * @param userId  用户ID
     * @param payload 自定义载荷数据（可选）
     * @return JWT token 字符串
     */
    public String generateToken(Long userId, Map<String, Object> payload) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration * 1000);

        return JWT.create()
                .setHeader(JWTHeader.ALGORITHM, "HS256")
                .setHeader(JWTHeader.TYPE, "JWT")
                .setPayload(RegisteredPayload.ISSUED_AT, now)
                .setPayload(RegisteredPayload.EXPIRES_AT, expireDate)
                .setPayload("userId", userId)
                .setPayload("data", payload)
                .setKey(secret.getBytes(StandardCharsets.UTF_8))
                .sign();
    }

    /**
     * 生成 token（无自定义载荷）
     *
     * @param userId 用户ID
     * @return JWT token 字符串
     */
    public String generateToken(Long userId) {
        return generateToken(userId, null);
    }

    /**
     * 验证 token（签名有效 且 未过期）
     *
     * @param token JWT token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        if (!StringUtils.hasLength(token)) {
            return false;
        }
        try {
            // 1. 验证签名
            if (!JWTUtil.verify(token, secret.getBytes(StandardCharsets.UTF_8))) {
                return false;
            }
            // 2. 验证未过期
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 token 中获取用户ID
     *
     * @param token JWT token
     * @return 用户ID（解析失败返回 null）
     */
    public Long getUserIdFromToken(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            Object userId = jwt.getPayload("userId");
            return userId != null ? Long.valueOf(userId.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 token 中获取自定义数据
     *
     * @param token JWT token
     * @param key   数据键
     * @return 数据值（解析失败返回 null）
     */
    public Object getDataFromToken(String token, String key) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            Map<String, Object> data = (Map<String, Object>) jwt.getPayload("data");
            return data != null ? data.get(key) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 token 中获取过期时间
     *
     * @param token JWT token
     * @return 过期时间（解析失败返回 null）
     */
    public Date getExpirationFromToken(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            Object expireAt = jwt.getPayload(RegisteredPayload.EXPIRES_AT);
            return expireAt instanceof Date ? (Date) expireAt : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查 token 是否已过期
     *
     * @param token JWT token
     * @return true=已过期，false=未过期
     */
    public boolean isTokenExpired(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            Date expireDate = (Date) jwt.getPayload(RegisteredPayload.EXPIRES_AT);
            return expireDate != null && expireDate.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}