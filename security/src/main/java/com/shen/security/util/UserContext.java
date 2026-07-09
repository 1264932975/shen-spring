package com.shen.security.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

/**
 * 用户上下文工具类
 * 从请求上下文中获取当前登录用户信息
 */
@UtilityClass
public class UserContext {

    /**
     * 获取当前登录用户ID
     * 由 JwtAuthenticationTokenFilter 从 JWT token 解析后设置到 request attribute
     */
    public static Long getCurrentUserId() {
        HttpServletRequest request = ClientInfoUtil.getCurrentRequest();
        return request != null ? (Long) request.getAttribute("userId") : null;
    }
}