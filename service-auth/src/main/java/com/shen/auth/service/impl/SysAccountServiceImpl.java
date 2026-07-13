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
import com.shen.common.constant.CommonConstant;
import com.shen.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            boolean exists = sysUserService.exists();
            if (exists) {
                return null;
            } else {
                return register(accountType, accountValue, password, SysRole.ROLE_ID_ADMIN);
            }
        }

        // 第三方登录无密码
        if (StringUtils.hasLength(account.getPassword()) && !passwordEncoder.matches(password, account.getPassword())) {
            return null;
        }

        account.setPassword(null);
        return account;
    }


    /***
     * @description: 这地方用起来重点要记得。
     * 如何要做登录即注册，依旧使用本套框架，需要严格注意管理账号和客户端账号登录逻辑。避免管理账号也进行了登录即注册。
     * @param: accountType
    accountValue
    password
    roleId
     * @return: com.shen.auth.entity.SysAccount
     * @author shield
     * @date: 2026/7/10 16:40
     */
    @Override
    @Transactional
    public SysAccount register(Integer accountType, String accountValue, String password, Long roleId) {
        // 检查账号是否已存在
        SysAccount exist = findByAccount(accountType, accountValue);
        if (exist != null) {
            throw new BusinessException("该账号已存在");
        }

        // 1. 创建用户
        SysUser user = new SysUser();
        user.setStatus(CommonConstant.STATUS_NORMAL);
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
        account.setStatus(CommonConstant.STATUS_NORMAL);
        super.save(account);

        // 4. 关联角色
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(roleId);
        sysUserRoleService.save(userRole);

        return account;
    }

    @Override
    public void deleteByUserId(Long userId) {
        super.lambdaUpdate()
                .eq(SysAccount::getUserId, userId)
                .remove();
    }

    @Override
    @Transactional
    public SysAccount bind(Long userId, Integer accountType, String accountValue, String password) {
        // 检查该账号值是否已被绑定（复用 findByAccount）
        SysAccount exist = findByAccount(accountType, accountValue);
        if (exist != null) {
            if (exist.getUserId().equals(userId)) {
                throw new BusinessException("已绑定该类型账号");
            }
            throw new BusinessException("该账号已被其他用户绑定");
        }

        SysAccount account = new SysAccount();
        account.setUserId(userId);
        account.setAccountType(accountType);
        account.setAccountValue(accountValue);
        if (StringUtils.hasLength(password)) {
            account.setPassword(passwordEncoder.encode(password));
        }
        account.setStatus(CommonConstant.STATUS_NORMAL);
        super.save(account);
        return account;
    }

    @Override
    @Transactional
    public void unbind(Long userId, Integer accountType) {
        // 检查是否是最后一个账号
        long count = super.lambdaQuery()
                .eq(SysAccount::getUserId, userId)
                .count();
        if (count <= 1) {
            throw new BusinessException("至少保留一个账号");
        }

        super.lambdaUpdate()
                .eq(SysAccount::getUserId, userId)
                .eq(SysAccount::getAccountType, accountType)
                .remove();
    }

    @Override
    public Map<Long, List<SysAccount>> getAccountsByUserIds(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return new HashMap<>();
        }
        return super.lambdaQuery()
                .in(SysAccount::getUserId, userIds)
                .list()
                .stream()
                .collect(Collectors.groupingBy(SysAccount::getUserId));
    }
}