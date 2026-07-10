package com.shen.auth.service;

import com.shen.auth.entity.SysAccount;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author shield
 * @description 针对表【sys_account(账号表)】的数据库操作Service
 * @createDate 2026-07-10 10:30:01
 */
public interface SysAccountService extends IService<SysAccount> {

    /**
     * 根据账号类型和账号值查找账号
     */
    SysAccount findByAccount(Integer accountType, String accountValue);

    /**
     * 登录
     */
    SysAccount login(Integer accountType, String accountValue, String password);

    /**
     * 注册（首次登录自动注册）
     */
    SysAccount register(Integer accountType, String accountValue, String password, Long roleId);
}