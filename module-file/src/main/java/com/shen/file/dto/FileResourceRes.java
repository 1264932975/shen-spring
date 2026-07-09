package com.shen.file.dto;

import lombok.Data;

/**
 * 文件上传响应
 */
@Data
public class FileResourceRes {

    /**
     * 文件ID
     */
    private Long id;

    /**
     * 主图访问URL
     */
    private String url;

    /**
     * 缩略图访问URL（仅图片文件有值）
     */
    private String thumbUrl;
}