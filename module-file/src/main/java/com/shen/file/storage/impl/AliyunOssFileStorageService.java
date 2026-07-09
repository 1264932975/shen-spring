package com.shen.file.storage.impl;

import com.shen.file.storage.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * 阿里云 OSS 对象存储实现
 * 通过配置 files.storage.type=aliyun-oss 启用
 */
@Component
@ConditionalOnProperty(name = "files.storage.type", havingValue = "aliyun-oss")
@Slf4j
public class AliyunOssFileStorageService implements FileStorageService {

    @Override
    public void upload(InputStream inputStream, String path) {
        // TODO: 实现阿里云 OSS 上传逻辑
        log.info("阿里云OSS文件上传：{}", path);
    }

    @Override
    public void delete(String path) {
        // TODO: 实现阿里云 OSS 删除逻辑
        log.info("阿里云OSS文件删除：{}", path);
    }

    @Override
    public String getUrl(String path) {
        // TODO: 返回阿里云 OSS 文件访问URL
        return path;
    }
}