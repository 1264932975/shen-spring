package com.shen.security.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 客户端信息工具类
 * 从请求上下文获取客户端版本、平台等信息
 */
@UtilityClass
public class ClientInfoUtil {

    // 请求头常量
    public static final String HEADER_CLIENT_VERSION = "X-Client-Version";
    public static final String HEADER_CLIENT_PLATFORM = "X-Client-Platform";

    // 请求属性常量
    public static final String CLIENT_VERSION = "clientVersion";
    public static final String CLIENT_PLATFORM = "clientPlatform";

    /**
     * 获取当前 HttpServletRequest
     */
    public static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return requestAttributes != null ? requestAttributes.getRequest() : null;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /**
     * 获取客户端版本号
     * 从请求属性中获取（由 JwtAuthenticationTokenFilter 从请求头解析后设置）
     */
    public static String getClientVersion() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? (String) request.getAttribute(CLIENT_VERSION) : null;
    }

    /**
     * 获取客户端平台信息
     * 从请求属性中获取（由 JwtAuthenticationTokenFilter 从请求头解析后设置）
     */
    public static String getClientPlatform() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? (String) request.getAttribute(CLIENT_PLATFORM) : null;
    }
}