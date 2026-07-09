package com.shen.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.file.entity.FileResource;
import com.shen.file.mapper.FileResourceMapper;
import com.shen.file.service.FileResourceService;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 文件资源 Service 实现
 */
@Service
public class FileResourceServiceImpl extends ServiceImpl<FileResourceMapper, FileResource>
        implements FileResourceService {

    @Override
    public FileResource saveFileResource(String path, String thumbPath) {
        FileResource fileResource = new FileResource();
        fileResource.setPath(path);
        fileResource.setThumbPath(thumbPath);
        fileResource.setRefCount(1);
        this.save(fileResource);
        return fileResource;
    }

    @Override
    public void incrementRefCount(Long id) {
        this.lambdaUpdate()
                .setSql("ref_count = ref_count + 1")
                .eq(FileResource::getId, id)
                .update();
    }

    @Override
    public void decrementRefCount(Long id) {
        this.lambdaUpdate()
                .setSql("ref_count = ref_count - 1")
                .eq(FileResource::getId, id)
                .update();
    }

    @Override
    public Page<FileResource> pageRefZero(Integer currentPage, Integer pageSize, Date bufferDate) {
        LambdaQueryWrapper<FileResource> qw = new LambdaQueryWrapper<>();
        qw.eq(FileResource::getRefCount, 0)
                .lt(FileResource::getCreateTime, bufferDate);
        Page<FileResource> page = new Page<>(currentPage, pageSize);
        return this.page(page, qw);
    }
}