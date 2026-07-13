package com.shen.auth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shen.auth.dto.SysUserDetailDTO;
import com.shen.auth.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author shield
* @description 针对表【sys_user(用户表)】的数据库操作Service
* @createDate 2026-07-10 10:30:01
*/
public interface SysUserService extends IService<SysUser> {

    /**
     * 分页查询用户列表（聚合 User + Profile + Account）
     */
    Page<SysUserDetailDTO> getPage(Integer currentPage, Integer pageSize, String accountValue, Integer accountType, Integer status);

    /**
     * 添加用户
     */
    SysUser add(SysUser user);

    /**
     * 修改用户
     */
    SysUser update(SysUser user);

    /**
     * 修改状态
     */
    void changeStatus(Long id, Integer status);

    /**
     * 注销用户（软删除，标记为删除状态）
     */
    void cancel(Long id);

    /**
     * 判断用户表是否存在数据
     */
    boolean exists();
}