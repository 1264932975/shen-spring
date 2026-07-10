package com.shen.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shen.framework.base.BaseEntity;
import lombok.Data;

/**
 * 角色表
 * @TableName sys_role
 */
@TableName(value ="sys_role")
@Data
public class SysRole extends BaseEntity {
    /**
     * 角色编码（如 ADMIN、APP_USER）
     */
    private String roleCode;

    /**
     * 角色名称（如 管理员、小程序用户）
     */
    private String roleName;

    /**
     * 角色类型：1-管理后台 2-小程序/APP 3-APP
     */
    private Integer roleType;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 状态：0-禁用 1-正常
     */
    private Integer status;
}