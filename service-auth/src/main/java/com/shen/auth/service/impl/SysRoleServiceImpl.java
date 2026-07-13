package com.shen.auth.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.auth.entity.SysRole;
import com.shen.auth.service.SysRoleMenuService;
import com.shen.auth.service.SysRoleService;
import com.shen.auth.service.SysUserRoleService;
import com.shen.auth.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
* @author shield
* @description 针对表【sys_role(角色表)】的数据库操作Service实现
* @createDate 2026-07-10 10:30:01
*/
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole>
    implements SysRoleService{

    private final SysRoleMenuService sysRoleMenuService;
    private final SysUserRoleService sysUserRoleService;

    @Override
    public Page<SysRole> getPage(Integer currentPage, Integer pageSize, String roleName, Integer roleType) {
        Page<SysRole> page = new Page<>(currentPage, pageSize);
        return super.lambdaQuery()
                .like(StringUtils.hasLength(roleName), SysRole::getRoleName, roleName)
                .eq(roleType != null, SysRole::getRoleType, roleType)
                .orderByAsc(SysRole::getCreateTime)
                .page(page);
    }

    @Override
    public SysRole add(SysRole role) {
        super.save(role);
        return role;
    }

    @Override
    public SysRole update(SysRole role) {
        super.updateById(role);
        return role;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        super.removeById(id);
        // 删除角色菜单关联
        sysRoleMenuService.deleteByRoleId(id);
        // 删除用户角色关联
        sysUserRoleService.deleteByRoleId(id);
    }

    @Override
    public boolean hasRoleType(Long userId, Integer roleType) {
        List<Long> roleIds = sysUserRoleService.getRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return false;
        }
        return super.lambdaQuery()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getRoleType, roleType)
                .count() > 0;
    }
}