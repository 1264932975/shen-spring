package com.shen.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.auth.entity.SysUserRole;
import com.shen.auth.service.SysUserRoleService;
import com.shen.auth.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
* @author shield
* @description 针对表【sys_user_role(用户角色关联表)】的数据库操作Service实现
* @createDate 2026-07-10 10:30:01
*/
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole>
    implements SysUserRoleService{

    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        return super.lambdaQuery()
                .eq(SysUserRole::getUserId, userId)
                .list()
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 先删除旧的
        super.lambdaUpdate()
                .eq(SysUserRole::getUserId, userId)
                .remove();
        
        // 再添加新的
        if (roleIds != null && !roleIds.isEmpty()) {
            List<SysUserRole> userRoles = roleIds.stream()
                    .map(roleId -> {
                        SysUserRole ur = new SysUserRole();
                        ur.setUserId(userId);
                        ur.setRoleId(roleId);
                        return ur;
                    })
                    .collect(Collectors.toList());
            super.saveBatch(userRoles);
        }
    }

    @Override
    public void deleteByUserId(Long userId) {
        super.lambdaUpdate()
                .eq(SysUserRole::getUserId, userId)
                .remove();
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        super.lambdaUpdate()
                .eq(SysUserRole::getRoleId, roleId)
                .remove();
    }
}