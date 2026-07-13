package com.shen.auth.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 用户详情聚合DTO（User + Profile + Account）
 */
@Data
public class SysUserDetailDTO {

    private Long id;

    private Integer status;

    private Date createTime;

    // Profile 信息
    private String nickname;

    private Long avatar;

    private String realName;

    private Integer gender;

    private String phone;

    private String email;

    // Account 列表（一个用户可能有多个账号）
    private List<AccountInfo> accounts;

    @Data
    public static class AccountInfo {
        private Integer accountType;
        private String accountValue;
        private Integer status;
    }
}