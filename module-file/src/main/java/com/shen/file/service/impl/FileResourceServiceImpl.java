package com.shen.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.file.entity.FileResource;
import com.shen.file.service.FileResourceService;
import com.shen.file.mapper.FileResourceMapper;
import org.springframework.stereotype.Service;

/**
* @author shield
* @description 针对表【file_resource(文件资源表)】的数据库操作Service实现
* @createDate 2026-07-09 11:55:29
*/
@Service
public class FileResourceServiceImpl extends ServiceImpl<FileResourceMapper, FileResource>
    implements FileResourceService{

}




