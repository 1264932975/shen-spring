package com.shen.auth.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.shen.auth.dto.SysUserDetailDTO;
import com.shen.auth.entity.SysAccount;
import com.shen.auth.entity.SysUser;
import com.shen.auth.entity.SysUserProfile;
import com.shen.auth.service.SysUserService;
import com.shen.auth.mapper.SysUserMapper;
import com.shen.common.constant.CommonConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
* @author shield
* @description 针对表【sys_user(用户表)】的数据库操作Service实现
* @createDate 2026-07-10 10:30:01
*/
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends MPJBaseServiceImpl<SysUserMapper, SysUser>
    implements SysUserService{

    @Override
    public Page<SysUserDetailDTO> getPage(Integer currentPage, Integer pageSize, String accountValue, Integer accountType, Integer status) {
        Page<SysUserDetailDTO> page = new Page<>(currentPage, pageSize);

        // 先查用户+profile
        MPJLambdaWrapper<SysUser> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(SysUser.class);
        wrapper.selectAs(SysUserProfile::getNickname, SysUserDetailDTO::getNickname);
        wrapper.selectAs(SysUserProfile::getAvatar, SysUserDetailDTO::getAvatar);
        wrapper.selectAs(SysUserProfile::getRealName, SysUserDetailDTO::getRealName);
        wrapper.selectAs(SysUserProfile::getGender, SysUserDetailDTO::getGender);
        wrapper.selectAs(SysUserProfile::getPhone, SysUserDetailDTO::getPhone);
        wrapper.selectAs(SysUserProfile::getEmail, SysUserDetailDTO::getEmail);
        wrapper.leftJoin(SysUserProfile.class, SysUserProfile::getId, SysUser::getId);

        //如果要账号或账号类型查询的话
        if (StringUtils.hasLength(accountValue) || accountType != null) {
            wrapper.leftJoin(SysAccount.class, SysAccount::getUserId, SysUser::getId);
            wrapper.like(StringUtils.hasLength(accountValue), SysAccount::getAccountValue, accountValue);
            wrapper.eq(accountType != null, SysAccount::getAccountType, accountType);
        }

        wrapper.eq(status != null, SysUser::getStatus, status);
        wrapper.orderByDesc(SysUser::getCreateTime);

        return super.selectJoinListPage(page, SysUserDetailDTO.class, wrapper);
    }

    @Override
    public SysUser add(SysUser user) {
        user.setStatus(CommonConstant.STATUS_NORMAL);
        super.save(user);
        return user;
    }

    @Override
    public SysUser update(SysUser user) {
        super.updateById(user);
        return user;
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        super.lambdaUpdate()
                .eq(SysUser::getId, id)
                .set(SysUser::getStatus, status)
                .update();
    }

    @Override
    public void cancel(Long id) {
        // 用户标记为删除状态
        super.lambdaUpdate()
                .eq(SysUser::getId, id)
                .set(SysUser::getStatus, CommonConstant.STATUS_DELETED)
                .update();
    }

    @Override
    public boolean exists() {
        return super.lambdaQuery()
                .select(SysUser::getId)
                .last("LIMIT 1")
                .one() != null;
    }
}