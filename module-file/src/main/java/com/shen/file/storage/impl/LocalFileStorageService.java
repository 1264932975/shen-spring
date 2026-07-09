package com.shen.file.storage.impl;

import cn.hutool.core.io.FileUtil;
import com.shen.file.storage.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * 本地文件系统存储实现
 * 通过配置 files.storage.type=local 启用
 */
@Component
@ConditionalOnProperty(name = "files.storage.type", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    @Value("${files.upload.path}")
    private String uploadPath;

    @Value("${files.upload.url}")
    private String accessUrl;

    @Override
    public void upload(InputStream inputStream, String path) {
        FileUtil.writeFromStream(inputStream, uploadPath + path);
        log.info("文件上传成功：{}", path);
    }

    @Override
    public void delete(String path) {
        boolean deleted = FileUtil.del(uploadPath + path);
        if (deleted) {
            log.info("文件删除成功：{}", path);
        } else {
            log.warn("文件删除失败或不存在：{}", path);
        }
    }

    @Override
    public String getUrl(String path) {
        return accessUrl + path;
    }
}