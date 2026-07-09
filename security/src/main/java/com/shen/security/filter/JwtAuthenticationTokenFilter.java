package com.shen.security.filter;

import cn.hutool.core.util.StrUtil;
import com.shen.security.util.ClientInfoUtil;
import com.shen.security.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * 拦截请求，解析 JWT token，将用户 ID 设置到 request attribute
 * 注意：此过滤器只负责解析 token，不设置 SecurityContext（由业务模块决定权限）
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 提取客户端信息并设置到 request attribute
        String clientVersion = request.getHeader(ClientInfoUtil.HEADER_CLIENT_VERSION);
        String clientPlatform = request.getHeader(ClientInfoUtil.HEADER_CLIENT_PLATFORM);

        if (StrUtil.isNotBlank(clientVersion)) {
            request.setAttribute(ClientInfoUtil.CLIENT_VERSION, clientVersion);
        }
        if (StrUtil.isNotBlank(clientPlatform)) {
            request.setAttribute(ClientInfoUtil.CLIENT_PLATFORM, clientPlatform);
        }

        // 提取并验证 token
        String token = extractToken(request);
        if (StrUtil.isBlank(token) || !jwtTokenUtil.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 解析用户 ID 并设置到 request attribute，供业务模块复用
        Long userId = jwtTokenUtil.getUserIdFromToken(token);
        if (userId != null) {
            request.setAttribute("userId", userId);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从 Authorization 请求头提取 token
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StrUtil.isNotBlank(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}