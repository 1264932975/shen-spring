package com.shen.auth.controller.open;

import com.shen.auth.service.SysAccountService;
import com.shen.auth.service.SysUserService;
import com.shen.security.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/open/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final SysAccountService sysAccountService;

    @Transactional
    @PutMapping("/cancel")
    public ResponseEntity<Object> cancel() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body("未登录");
        }
        sysUserService.cancel(userId);
        sysAccountService.deleteByUserId(userId);
        return ResponseEntity.ok("注销成功");
    }
}