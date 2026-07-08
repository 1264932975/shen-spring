package com.shen.framework.base;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类，所有数据库实体都必须继承此类
 * 包含三个固定字段：id、createTime、updateTime
 * 时间字段由数据库层自动维护（DEFAULT CURRENT_TIMESTAMP / ON UPDATE CURRENT_TIMESTAMP）
 */
@Data
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID（使用雪花算法生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建时间（数据库 DEFAULT CURRENT_TIMESTAMP 自动填充）
     */
    private LocalDateTime createTime;

    /**
     * 更新时间（数据库 ON UPDATE CURRENT_TIMESTAMP 自动更新）
     */
    private LocalDateTime updateTime;
}
