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
    public ResponseEntity<Object> add(@RequestBody SysUser user) {
        return ResponseEntity.ok(sysUserService.add(user));
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
}