package com.shen.auth.controller.open;

import com.shen.auth.entity.SysAccount;
import com.shen.auth.entity.SysUser;
import com.shen.auth.service.SysAccountService;
import com.shen.auth.service.SysUserService;
import com.shen.security.util.JwtTokenUtil;
import com.shen.security.util.UserContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/open/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final SysAccountService sysAccountService;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody LoginReq req) {
        if (req.getAccountType() == null) {
            return ResponseEntity.badRequest().body("账号类型不能为空");
        }
        if (!StringUtils.hasLength(req.getAccountValue())) {
            return ResponseEntity.badRequest().body("账号不能为空");
        }
        // 只有账号密码登录才需要密码
        if (req.getAccountType() == SysAccount.ACCOUNT_TYPE_USERNAME
                && !StringUtils.hasLength(req.getPassword())) {
            return ResponseEntity.badRequest().body("密码不能为空");
        }

        SysAccount account = sysAccountService.login(
                req.getAccountType(), req.getAccountValue(), req.getPassword());
        if (account == null) {
            return ResponseEntity.badRequest().body("账号或密码错误");
        }

        String token = jwtTokenUtil.generateToken(account.getUserId(), null);
        LoginRes loginRes = new LoginRes();
        loginRes.setToken(token);
        loginRes.setUserId(account.getUserId());
        return ResponseEntity.ok(loginRes);
    }

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

    @Data
    public static class LoginReq {
        private Integer accountType;
        private String accountValue;
        private String password;
    }

    @Data
    public static class LoginRes {
        private String token;
        private Long userId;
    }
}