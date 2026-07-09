package com.shen.file.storage;

import java.io.InputStream;

/**
 * 文件存储接口
 * <p>
 * 预留扩展：本地存储、MinIO、阿里云 OSS 等统一抽象
 */
public interface FileStorageService {

    /**
     * 上传文件
     *
     * @param inputStream 文件输入流
     * @param path        目标路径（相对路径）
     */
    void upload(InputStream inputStream, String path);

    /**
     * 删除文件
     *
     * @param path 文件路径（相对路径）
     */
    void delete(String path);

    /**
     * 获取文件访问URL
     *
     * @param path 文件路径（相对路径）
     * @return 完整访问URL
     */
    String getUrl(String path);
}