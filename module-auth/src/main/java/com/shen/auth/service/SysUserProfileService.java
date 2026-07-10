package com.shen.auth.service;

import com.shen.auth.entity.SysUserProfile;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author shield
* @description 针对表【sys_user_profile(用户基础信息表)】的数据库操作Service
* @createDate 2026-07-10 10:30:01
*/
public interface SysUserProfileService extends IService<SysUserProfile> {

    /**
     * 根据用户ID获取资料
     */
    SysUserProfile getByUserId(Long userId);

    /**
     * 修改用户资料
     */
    void updateInfo(SysUserProfile profile);
}