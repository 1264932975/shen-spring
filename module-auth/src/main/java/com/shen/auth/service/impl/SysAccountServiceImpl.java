package com.shen.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.auth.entity.SysAccount;
import com.shen.auth.service.SysAccountService;
import com.shen.auth.mapper.SysAccountMapper;
import org.springframework.stereotype.Service;

/**
* @author shield
* @description 针对表【sys_account(账号表)】的数据库操作Service实现
* @createDate 2026-07-10 10:30:01
*/
@Service
public class SysAccountServiceImpl extends ServiceImpl<SysAccountMapper, SysAccount>
    implements SysAccountService{

}




