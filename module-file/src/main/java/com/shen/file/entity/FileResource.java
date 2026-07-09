package com.shen.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shen.framework.base.BaseEntity;
import lombok.Data;

/**
 * 文件资源表
 * @TableName file_resource
 */
@TableName(value ="file_resource")
@Data
public class FileResource extends BaseEntity {
    /**
     * 文件相对路径（如 /2026/07/09/123456.jpg）
     */
    private String path;

    /**
     * 缩略图相对路径（如 /2026/07/09/123456_thumb.jpg）
     */
    private String thumbPath;

    /**
     * 引用计数
     */
    private Integer refCount;
}