package com.shen.auth.service;

import com.shen.auth.entity.SysUserRole;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author shield
* @description 针对表【sys_user_role(用户角色关联表)】的数据库操作Service
* @createDate 2026-07-10 10:30:01
*/
public interface SysUserRoleService extends IService<SysUserRole> {

    /**
     * 获取用户的角色ID列表
     */
    List<Long> getRoleIdsByUserId(Long userId);

    /**
     * 分配角色（先删后增）
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 根据用户ID删除关联
     */
    void deleteByUserId(Long userId);
}