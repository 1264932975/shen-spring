package com.shen.auth.service;

import com.shen.auth.entity.SysRoleMenu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author shield
* @description 针对表【sys_role_menu(角色菜单关联表)】的数据库操作Service
* @createDate 2026-07-10 10:30:01
*/
public interface SysRoleMenuService extends IService<SysRoleMenu> {

    /**
     * 分配菜单权限（先删后增）
     */
    void assignMenus(Long roleId, List<Long> menuIds);

    /**
     * 获取角色的菜单ID列表
     */
    List<Long> getMenuIdsByRoleId(Long roleId);

    /**
     * 根据角色ID删除关联
     */
    void deleteByRoleId(Long roleId);

    /**
     * 根据菜单ID删除关联
     */
    void deleteByMenuId(Long menuId);
}