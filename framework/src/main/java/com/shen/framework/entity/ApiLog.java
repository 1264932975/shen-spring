package com.shen.framework.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shen.framework.base.BaseEntity;
import lombok.Data;

/**
 * 接口日志表
 * @TableName api_log
 */
@TableName(value ="api_log")
@Data
public class ApiLog extends BaseEntity {
    /**
     * 主键 ID（日志表用自增，覆盖 BaseEntity 的雪花ID）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 服务名称
     */
    private String service;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 请求路径
     */
    private String uri;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 操作人ID（未登录/系统调用时为null）
     */
    private Long operatorId;

    /**
     * 操作人角色ID
     */
    private Long operatorRoleId;

    /**
     * 客户端版本
     */
    private String clientVersion;

    /**
     * 客户端平台
     */
    private String clientPlatform;

    /**
     * Query参数
     */
    private String params;

    /**
     * Request Body
     */
    private String body;

    /**
     * 响应结果
     */
    private String result;

    /**
     * 耗时(毫秒)
     */
    private Long time;
}