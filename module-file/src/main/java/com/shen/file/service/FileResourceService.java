package com.shen.file.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shen.file.entity.FileResource;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Date;

/**
 * 文件资源 Service
 */
public interface FileResourceService extends IService<FileResource> {

    /**
     * 保存文件资源
     *
     * @param path 文件相对路径
     * @param thumbPath 缩略图相对路径（可为空）
     * @return 文件资源实体
     */
    FileResource saveFileResource(String path, String thumbPath);

    /**
     * 增加引用计数（索引+1）
     *
     * @param id 文件资源ID
     */
    void incrementRefCount(Long id);

    /**
     * 减少引用计数（索引-1）
     *
     * @param id 文件资源ID
     */
    void decrementRefCount(Long id);

    /**
     * 分页查询未被引用且超时的记录
     *
     * @param currentPage 当前页
     * @param pageSize 每页大小
     * @param bufferDate 缓冲时间（创建时间早于此时间的记录）
     * @return 分页结果
     */
    Page<FileResource> pageRefZero(Integer currentPage, Integer pageSize, Date bufferDate);
}