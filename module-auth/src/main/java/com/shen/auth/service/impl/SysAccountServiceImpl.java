package com.shen.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.auth.entity.SysAccount;
import com.shen.auth.entity.SysRole;
import com.shen.auth.entity.SysUser;
import com.shen.auth.entity.SysUserProfile;
import com.shen.auth.entity.SysUserRole;
import com.shen.auth.mapper.SysAccountMapper;
import com.shen.auth.service.SysAccountService;
import com.shen.auth.service.SysUserProfileService;
import com.shen.auth.service.SysUserRoleService;
import com.shen.auth.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * @author shield
 * @description 针对表【sys_account(账号表)】的数据库操作Service实现
 * @createDate 2026-07-10 10:30:01
 */
@Service
@RequiredArgsConstructor
public class SysAccountServiceImpl extends ServiceImpl<SysAccountMapper, SysAccount>
        implements SysAccountService {

    private final PasswordEncoder passwordEncoder;
    private final SysUserService sysUserService;
    private final SysUserProfileService sysUserProfileService;
    private final SysUserRoleService sysUserRoleService;

    @Override
    public SysAccount findByAccount(Integer accountType, String accountValue) {
        return super.lambdaQuery()
                .eq(SysAccount::getAccountType, accountType)
                .eq(SysAccount::getAccountValue, accountValue)
                .one();
    }

    @Override
    @Transactional
    public SysAccount login(Integer accountType, String accountValue, String password) {
        SysAccount account = findByAccount(accountType, accountValue);

        if (account == null) {
            // 没查到，如果数据库为空，第一个人进行注册操作
            boolean exists = sysUserService.lambdaQuery().select(SysUser::getId).last("LIMIT 1").one() != null;
            if (exists) {
                return null;
            } else {
                return  register(accountType, accountValue, password, SysRole.ROLE_ID_ADMIN);
            }
        }

        // 第三方登录无密码
        if (StringUtils.hasLength(account.getPassword()) && !passwordEncoder.matches(password, account.getPassword())) {
            return null;
        }

        account.setPassword(null);
        return account;
    }

    @Override
    @Transactional
    public SysAccount register(Integer accountType, String accountValue, String password, Long roleId) {
        // 1. 创建用户
        SysUser user = new SysUser();
        user.setStatus(1);
        sysUserService.save(user);

        // 2. 创建用户基础信息
        SysUserProfile profile = new SysUserProfile();
        profile.setId(user.getId());
        sysUserProfileService.save(profile);

        // 3. 创建账号
        SysAccount account = new SysAccount();
        account.setUserId(user.getId());
        account.setAccountType(accountType);
        account.setAccountValue(accountValue);
        if (StringUtils.hasLength(password)) {
            account.setPassword(passwordEncoder.encode(password));
        }
        account.setStatus(1);
        super.save(account);

        // 4. 关联角色
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(roleId);
        sysUserRoleService.save(userRole);

        return account;
    }
}