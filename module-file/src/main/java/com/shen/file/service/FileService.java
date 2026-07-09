package com.shen.file.service;

import com.shen.file.dto.FileResourceRes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件上传服务接口
 */
public interface FileService {

    /**
     * 上传文件（支持图片压缩 + 缩略图生成）
     *
     * @param file 上传的文件
     * @return 文件访问URL（含主图和缩略图）
     * @throws IOException 文件操作异常
     */
    FileResourceRes upload(MultipartFile file) throws IOException;

    /**
     * 减少引用计数（业务方删除记录时调用）
     */
    void delete(Long fileId);

    /**
     * 增加引用计数（业务方新增引用时调用）
     */
    void add(Long fileId);

    /**
     * 根据 ID 获取文件访问 URL
     */
    String getUrlById(Long id);

    /**
     * 根据 ID 获取缩略图 URL
     */
    String getThumbUrlById(Long id);
}