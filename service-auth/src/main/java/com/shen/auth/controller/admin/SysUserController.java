package com.shen.auth.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shen.auth.dto.SysUserDetailDTO;
import com.shen.auth.entity.SysAccount;
import com.shen.auth.entity.SysUser;
import com.shen.auth.service.SysAccountService;
import com.shen.auth.service.SysUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/sysUser")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;
    private final SysAccountService sysAccountService;

    @PostMapping("/page")
    public ResponseEntity<Object> page(@RequestBody PageReq req) {
        Page<SysUserDetailDTO> result = sysUserService.getPage(
                req.getCurrentPage(), req.getPageSize(),
                req.getAccountValue(), req.getAccountType(), req.getStatus());

        // 组装账号列表
        List<Long> userIds = result.getRecords().stream()
                .map(SysUserDetailDTO::getId)
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(userIds)) {
            Map<Long, List<SysAccount>> accountMap = sysAccountService.getAccountsByUserIds(userIds);
            for (SysUserDetailDTO dto : result.getRecords()) {
                List<SysAccount> userAccounts = accountMap.get(dto.getId());
                if (userAccounts != null) {
                    List<SysUserDetailDTO.AccountInfo> accountInfos = userAccounts.stream()
                            .map(acc -> {
                                SysUserDetailDTO.AccountInfo info = new SysUserDetailDTO.AccountInfo();
                                info.setAccountType(acc.getAccountType());
                                info.setAccountValue(acc.getAccountValue());
                                info.setStatus(acc.getStatus());
                                return info;
                            })
                            .collect(Collectors.toList());
                    dto.setAccounts(accountInfos);
                }
            }
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/add")
    public ResponseEntity<Object> add(String accountValue) {
        if (!StringUtils.hasLength(accountValue)) {
            return ResponseEntity.badRequest().body("账号不能为空");
        }
        // 自动生成8位密码（大小写字母+数字+特殊符号）
        String password = generatePassword();
        // 创建用户并绑定账号
        SysAccount account = sysAccountService.register(1, accountValue, password, null);
        return ResponseEntity.ok(password);
    }

    @PutMapping("/update")
    public ResponseEntity<Object> update(@RequestBody SysUser user) {
        if (user.getId() == null) {
            return ResponseEntity.badRequest().body("id不能为空");
        }
        return ResponseEntity.ok(sysUserService.update(user));
    }

    @PutMapping("/changeStatus")
    public ResponseEntity<Object> changeStatus(Long id, Integer status) {
        if (id == null) {
            return ResponseEntity.badRequest().body("id不能为空");
        }
        sysUserService.changeStatus(id, status);
        return ResponseEntity.ok("修改成功");
    }

    @Data
    public static class PageReq {
        private Integer currentPage = 1;
        private Integer pageSize = 10;
        private String accountValue;
        private Integer accountType;
        private Integer status;
    }

    private String generatePassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*";
        String all = upper + lower + digits + special;
        java.util.Random random = new java.util.Random();
        StringBuilder sb = new StringBuilder(8);
        // 确保每种类型至少一个
        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(special.charAt(random.nextInt(special.length())));
        // 剩余4位随机
        for (int i = 0; i < 4; i++) {
            sb.append(all.charAt(random.nextInt(all.length())));
        }
        // 打乱顺序
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}