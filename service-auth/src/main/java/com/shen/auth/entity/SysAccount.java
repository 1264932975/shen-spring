package com.shen.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shen.framework.base.BaseEntity;
import lombok.Data;

/**
 * 账号表
 * @TableName sys_account
 */
@TableName(value ="sys_account")
@Data
public class SysAccount extends BaseEntity {

    /** 账号类型：1-用户名（2-手机号 3-邮箱 4-微信 5-APP 为示例，可按需扩展） */
    public static final int ACCOUNT_TYPE_USERNAME = 1;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 账号类型：1-用户名 2-手机号 3-邮箱 4-微信 5-APP
     */
    private Integer accountType;

    /**
     * 账号标识（用户名/手机号/邮箱/openid等）
     */
    private String accountValue;

    /**
     * 密码（BCrypt加密，第三方登录为空）
     */
    private String password;

    /**
     * 状态：0-禁用 1-正常
     */
    private Integer status;
}