package com.shen.auth.controller.admin;

import com.shen.auth.entity.SysRole;
import com.shen.auth.service.SysRoleService;
import com.shen.auth.service.SysUserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/sysRole")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;
    private final SysUserRoleService sysUserRoleService;

    @PostMapping("/page")
    public ResponseEntity<Object> page(@RequestBody PageReq req) {
        return ResponseEntity.ok(sysRoleService.getPage(
                req.getCurrentPage(), req.getPageSize(),
                req.getRoleName(), req.getRoleType()));
    }

    @PostMapping("/add")
    public ResponseEntity<Object> add(@RequestBody SysRole role) {
        if (!StringUtils.hasLength(role.getRoleName())) {
            return ResponseEntity.badRequest().body("角色名称不能为空");
        }
        return ResponseEntity.ok(sysRoleService.add(role));
    }

    @PutMapping("/update")
    public ResponseEntity<Object> update(@RequestBody SysRole role) {
        if (role.getId() == null) {
            return ResponseEntity.badRequest().body("id不能为空");
        }
        return ResponseEntity.ok(sysRoleService.update(role));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Object> delete(Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().body("id不能为空");
        }
        sysRoleService.delete(id);
        return ResponseEntity.ok("删除成功");
    }

    @GetMapping("/getRoleIdsByUserId")
    public ResponseEntity<Object> getRoleIdsByUserId(Long userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().body("userId不能为空");
        }
        return ResponseEntity.ok(sysUserRoleService.getRoleIdsByUserId(userId));
    }

    @PostMapping("/assignRoles")
    public ResponseEntity<Object> assignRoles(Long userId, @RequestBody List<Long> roleIds) {
        if (userId == null) {
            return ResponseEntity.badRequest().body("userId不能为空");
        }
        sysUserRoleService.assignRoles(userId, roleIds);
        return ResponseEntity.ok("分配成功");
    }

    @lombok.Data
    public static class PageReq {
        private Integer currentPage = 1;
        private Integer pageSize = 10;
        private String roleName;
        private Integer roleType;
    }
}