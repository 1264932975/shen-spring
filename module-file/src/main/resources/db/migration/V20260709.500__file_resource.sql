CREATE TABLE file_resource (
    id BIGINT NOT NULL COMMENT '文件ID（雪花ID）',
    path VARCHAR(500) NOT NULL COMMENT '文件相对路径（如 /2026/07/09/123456.jpg）',
    thumb_path VARCHAR(500) DEFAULT NULL COMMENT '缩略图相对路径（如 /2026/07/09/123456_thumb.jpg）',
    ref_count INT NOT NULL DEFAULT 0 COMMENT '引用计数',
    create_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_ref_count (ref_count),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB  COMMENT='文件资源表';