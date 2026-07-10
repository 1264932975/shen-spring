package com.shen.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.auth.dto.SysMenuTreeDTO;
import com.shen.auth.entity.SysMenu;
import com.shen.auth.entity.SysRoleMenu;
import com.shen.auth.service.SysMenuService;
import com.shen.auth.service.SysRoleMenuService;
import com.shen.auth.service.SysUserRoleService;
import com.shen.auth.mapper.SysMenuMapper;
import com.shen.common.constant.CommonConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author shield
* @description 针对表【sys_menu(菜单/权限表)】的数据库操作Service实现
* @createDate 2026-07-10 10:30:01
*/
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu>
    implements SysMenuService{

    private final SysRoleMenuService sysRoleMenuService;
    private final SysUserRoleService sysUserRoleService;

    @Override
    public List<SysMenuTreeDTO> getMenuTree(Long userId) {
        // 获取用户的所有角色ID
        List<Long> roleIds = sysUserRoleService.getRoleIdsByUserId(userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return new ArrayList<>();
        }
        
        // 获取角色的所有菜单ID
        List<Long> menuIds = new ArrayList<>();
        for (Long roleId : roleIds) {
            menuIds.addAll(sysRoleMenuService.getMenuIdsByRoleId(roleId));
        }
        
        if (CollectionUtils.isEmpty(menuIds)) {
            return new ArrayList<>();
        }
        
        // 获取用户有权限的菜单，按sort排序
        List<SysMenu> userMenus = lambdaQuery()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getStatus, CommonConstant.STATUS_NORMAL)
                .orderByAsc(SysMenu::getSort)
                .list();
        
        // 构建树形结构
        return buildTree(0L, userMenus);
    }

    @Override
    public List<String> getPermissionsByUserId(Long userId) {
        // 获取用户的所有角色ID
        List<Long> roleIds = sysUserRoleService.getRoleIdsByUserId(userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return new ArrayList<>();
        }
        
        // 获取角色的所有菜单ID
        List<Long> menuIds = new ArrayList<>();
        for (Long roleId : roleIds) {
            menuIds.addAll(sysRoleMenuService.getMenuIdsByRoleId(roleId));
        }
        
        // 获取菜单的权限标识
        if (CollectionUtils.isEmpty(menuIds)) {
            return new ArrayList<>();
        }
        
        return lambdaQuery()
                .select(SysMenu::getPermissionCode)
                .in(SysMenu::getId, menuIds)
                .isNotNull(SysMenu::getPermissionCode)
                .list()
                .stream()
                .map(SysMenu::getPermissionCode)
                .collect(Collectors.toList());
    }

    @Override
    public SysMenu add(SysMenu menu) {
        super.save(menu);
        return menu;
    }

    @Override
    public SysMenu update(SysMenu menu) {
        super.updateById(menu);
        return menu;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 删除所有子菜单的关联和子菜单
        deleteChildren(id);
        // 删除自己的角色菜单关联
        sysRoleMenuService.deleteByMenuId(id);
        // 删除自己
        super.removeById(id);
    }

    /**
     * 递归删除子菜单
     */
    private void deleteChildren(Long parentId) {
        List<SysMenu> children = lambdaQuery()
                .eq(SysMenu::getParentId, parentId)
                .list();
        
        for (SysMenu child : children) {
            deleteChildren(child.getId());
            // 删除子菜单的角色菜单关联
            sysRoleMenuService.deleteByMenuId(child.getId());
            // 删除子菜单
            super.removeById(child.getId());
        }
    }

    /**
     * 构建树形结构
     */
    private List<SysMenuTreeDTO> buildTree(Long parentId, List<SysMenu> allMenus) {
        List<SysMenuTreeDTO> tree = new ArrayList<>();
        
        for (SysMenu menu : allMenus) {
            if (menu.getParentId().equals(parentId)) {
                SysMenuTreeDTO dto = new SysMenuTreeDTO();
                BeanUtils.copyProperties(menu, dto);
                dto.setChildren(buildTree(menu.getId(), allMenus));
                tree.add(dto);
            }
        }
        
        return tree;
    }
}