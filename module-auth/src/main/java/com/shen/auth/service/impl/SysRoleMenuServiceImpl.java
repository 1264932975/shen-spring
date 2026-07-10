package com.shen.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.auth.entity.SysRoleMenu;
import com.shen.auth.service.SysRoleMenuService;
import com.shen.auth.mapper.SysRoleMenuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
* @author shield
* @description 针对表【sys_role_menu(角色菜单关联表)】的数据库操作Service实现
* @createDate 2026-07-10 10:30:01
*/
@Service
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu>
    implements SysRoleMenuService{

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        // 先删除旧的
        lambdaUpdate()
                .eq(SysRoleMenu::getRoleId, roleId)
                .remove();
        
        // 再添加新的
        if (menuIds != null && !menuIds.isEmpty()) {
            List<SysRoleMenu> roleMenus = menuIds.stream()
                    .map(menuId -> {
                        SysRoleMenu rm = new SysRoleMenu();
                        rm.setRoleId(roleId);
                        rm.setMenuId(menuId);
                        return rm;
                    })
                    .collect(Collectors.toList());
            saveBatch(roleMenus);
        }
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        return lambdaQuery()
                .eq(SysRoleMenu::getRoleId, roleId)
                .list()
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        lambdaUpdate()
                .eq(SysRoleMenu::getRoleId, roleId)
                .remove();
    }
}