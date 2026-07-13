package com.shen.auth.controller.admin;

import com.shen.auth.dto.SysMenuTreeDTO;
import com.shen.auth.entity.SysMenu;
import com.shen.auth.entity.SysRole;
import com.shen.auth.service.SysMenuService;
import com.shen.auth.service.SysRoleMenuService;
import com.shen.auth.service.SysUserRoleService;
import com.shen.security.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/sysMenu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;
    private final SysRoleMenuService sysRoleMenuService;
    private final SysUserRoleService sysUserRoleService;

    @GetMapping("/tree")
    public ResponseEntity<Object> tree() {
        Long userId = UserContext.getCurrentUserId();
        // 超级管理员返回全部菜单
        if (sysUserRoleService.getRoleIdsByUserId(userId).contains(SysRole.ROLE_ID_ADMIN)) {
            return ResponseEntity.ok(sysMenuService.getMenuTree(null));
        }
        return ResponseEntity.ok(sysMenuService.getMenuTree(userId));
    }

    @GetMapping("/list")
    public ResponseEntity<Object> list() {
        return ResponseEntity.ok(sysMenuService.getMenuTree(null));
    }

    @PostMapping("/add")
    public ResponseEntity<Object> add(@RequestBody SysMenu menu) {
        if (!StringUtils.hasLength(menu.getMenuName())) {
            return ResponseEntity.badRequest().body("菜单名称不能为空");
        }
        return ResponseEntity.ok(sysMenuService.add(menu));
    }

    @PutMapping("/update")
    public ResponseEntity<Object> update(@RequestBody SysMenu menu) {
        if (menu.getId() == null) {
            return ResponseEntity.badRequest().body("id不能为空");
        }
        return ResponseEntity.ok(sysMenuService.update(menu));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Object> delete(Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().body("id不能为空");
        }
        sysMenuService.delete(id);
        return ResponseEntity.ok("删除成功");
    }

    @PostMapping("/assignMenus")
    public ResponseEntity<Object> assignMenus(Long roleId, @RequestBody List<Long> menuIds) {
        if (roleId == null) {
            return ResponseEntity.badRequest().body("roleId不能为空");
        }
        sysRoleMenuService.assignMenus(roleId, menuIds);
        return ResponseEntity.ok("分配成功");
    }

    @GetMapping("/getMenuIdsByRoleId")
    public ResponseEntity<Object> getMenuIdsByRoleId(Long roleId) {
        if (roleId == null) {
            return ResponseEntity.badRequest().body("roleId不能为空");
        }
        return ResponseEntity.ok(sysRoleMenuService.getMenuIdsByRoleId(roleId));
    }
}