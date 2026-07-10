package com.shen.auth.service;

import com.shen.auth.dto.SysMenuTreeDTO;
import com.shen.auth.entity.SysMenu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author shield
* @description 针对表【sys_menu(菜单/权限表)】的数据库操作Service
* @createDate 2026-07-10 10:30:01
*/
public interface SysMenuService extends IService<SysMenu> {

    /**
     * 获取菜单树（根据用户权限过滤）
     */
    List<SysMenuTreeDTO> getMenuTree(Long userId);

    /**
     * 获取用户权限列表
     */
    List<String> getPermissionsByUserId(Long userId);

    /**
     * 添加菜单
     */
    SysMenu add(SysMenu menu);

    /**
     * 修改菜单
     */
    SysMenu update(SysMenu menu);

    /**
     * 删除菜单（包括子菜单）
     */
    void delete(Long id);
}