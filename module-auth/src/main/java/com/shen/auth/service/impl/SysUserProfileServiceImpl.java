package com.shen.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.auth.entity.SysUserProfile;
import com.shen.auth.service.SysUserProfileService;
import com.shen.auth.mapper.SysUserProfileMapper;
import org.springframework.stereotype.Service;

/**
* @author shield
* @description 针对表【sys_user_profile(用户基础信息表)】的数据库操作Service实现
* @createDate 2026-07-10 10:30:01
*/
@Service
public class SysUserProfileServiceImpl extends ServiceImpl<SysUserProfileMapper, SysUserProfile>
    implements SysUserProfileService{

}




