package com.shen.auth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shen.auth.entity.SysRole;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author shield
* @description 针对表【sys_role(角色表)】的数据库操作Service
* @createDate 2026-07-10 10:30:01
*/
public interface SysRoleService extends IService<SysRole> {

    /**
     * 分页查询角色列表
     */
    Page<SysRole> getPage(Integer currentPage, Integer pageSize, String roleName, Integer roleType);

    /**
     * 添加角色
     */
    SysRole add(SysRole role);

    /**
     * 修改角色
     */
    SysRole update(SysRole role);

    /**
     * 删除角色
     */
    void delete(Long id);

    /**
     * 判断用户是否有指定类型的角色
     */
    boolean hasRoleType(Long userId, Integer roleType);
}