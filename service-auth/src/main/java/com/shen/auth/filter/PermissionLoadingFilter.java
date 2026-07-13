package com.shen.auth.filter;

import com.shen.auth.entity.SysRole;
import com.shen.auth.service.SysMenuService;
import com.shen.auth.service.SysRoleService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionLoadingFilter extends OncePerRequestFilter {

    private final SysMenuService sysMenuService;
    private final SysRoleService sysRoleService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Long userId = (Long) request.getAttribute("userId");//此处的前置条件为，security提供的过滤器在此之前执行

        if (userId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 加载用户权限
        List<String> permissions = sysMenuService.getPermissionsByUserId(userId);

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }

        // 判断是否有管理后台角色，赋予 ROLE_ADMIN(同理，按照业务需求，多客户端多平台，可设置不同接口前缀拦截权限）先进行平台拦截，后进行菜单权限细粒度校验
        if (sysRoleService.hasRoleType(userId, SysRole.ROLE_TYPE_ADMIN)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        //TODO: 其他角色类型

               UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        filterChain.doFilter(request, response);
    }
}