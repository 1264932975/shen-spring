package com.shen.auth.service.impl;

import com.github.yulichang.base.MPJBaseServiceImpl;
import com.shen.auth.entity.SysUserProfile;
import com.shen.auth.service.SysUserProfileService;
import com.shen.auth.mapper.SysUserProfileMapper;
import com.shen.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author shield
 * @description 针对表【sys_user_profile(用户基础信息表)】的数据库操作Service实现
 * @createDate 2026-07-10 10:30:01
 */
@Service
@RequiredArgsConstructor
public class SysUserProfileServiceImpl extends MPJBaseServiceImpl<SysUserProfileMapper, SysUserProfile>
        implements SysUserProfileService{

    private final FileService fileService;

    @Override
    public SysUserProfile getByUserId(Long userId) {
        return lambdaQuery()
                .eq(SysUserProfile::getId, userId)
                .one();
    }

    @Override
    public void updateInfo(SysUserProfile profile) {
        SysUserProfile oldProfile = getByUserId(profile.getId());

        // 头像变更：旧头像减索引，新头像加索引
        if (profile.getAvatar() != null && oldProfile != null) {
            if (!profile.getAvatar().equals(oldProfile.getAvatar())) {
                // 旧头像减索引
                fileService.delete(oldProfile.getAvatar());
                // 新头像加索引
                fileService.add(profile.getAvatar());
            }
        }

        lambdaUpdate()
                .eq(SysUserProfile::getId, profile.getId())
                .set(StringUtils.hasLength(profile.getNickname()), SysUserProfile::getNickname, profile.getNickname())
                .set(profile.getAvatar() != null, SysUserProfile::getAvatar, profile.getAvatar())
                .set(StringUtils.hasLength(profile.getRealName()), SysUserProfile::getRealName, profile.getRealName())
                .set(profile.getGender() != null, SysUserProfile::getGender, profile.getGender())
                .set(StringUtils.hasLength(profile.getPhone()), SysUserProfile::getPhone, profile.getPhone())
                .set(StringUtils.hasLength(profile.getEmail()), SysUserProfile::getEmail, profile.getEmail())
                .update();
    }
}