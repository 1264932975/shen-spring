package com.shen.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.auth.entity.SysUser;
import com.shen.auth.service.SysUserService;
import com.shen.auth.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

/**
* @author shield
* @description 针对表【sys_user(用户表)】的数据库操作Service实现
* @createDate 2026-07-10 10:30:01
*/
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
    implements SysUserService{

}




