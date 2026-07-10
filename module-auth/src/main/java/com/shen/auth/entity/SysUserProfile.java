package com.shen.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shen.framework.base.BaseEntity;
import lombok.Data;

/**
 * 用户基础信息表
 * @TableName sys_user_profile
 */
@TableName(value ="sys_user_profile")
@Data
public class SysUserProfile extends BaseEntity {
    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像文件ID（关联 file_resource 表）
     */
    private Long avatar;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 性别：0-未知 1-男 2-女
     */
    private Integer gender;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;
}