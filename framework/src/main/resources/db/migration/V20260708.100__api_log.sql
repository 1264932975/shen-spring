CREATE TABLE api_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    service VARCHAR(100) COMMENT '服务名称',
    port INT COMMENT '端口',
    uri VARCHAR(500) NOT NULL COMMENT '请求路径',
    method VARCHAR(10) NOT NULL COMMENT '请求方法',
    operator_id BIGINT COMMENT '操作人ID（未登录/系统调用时为null）',
    operator_role_id BIGINT COMMENT '操作人角色ID',
    client_version VARCHAR(50) COMMENT '客户端版本',
    client_platform VARCHAR(50) COMMENT '客户端平台',
    params TEXT COMMENT 'Query参数',
    body TEXT COMMENT 'Request Body',
    result TEXT COMMENT '响应结果',
    time BIGINT COMMENT '耗时(毫秒)',
    create_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id)
) COMMENT='接口日志表';