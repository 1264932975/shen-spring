-- 用户表（只保留登录相关字段）
CREATE TABLE sys_user (
    id BIGINT NOT NULL COMMENT '用户ID（雪花ID）',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 用户基础信息表（扩展表）
CREATE TABLE sys_user_profile (
    id BIGINT NOT NULL COMMENT '用户ID（与 sys_user.id 一致）',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar BIGINT COMMENT '头像文件ID（关联 file_resource 表）',
    real_name VARCHAR(50) COMMENT '真实姓名',
    gender TINYINT DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础信息表';

-- 账号表（登录凭证，支持多种登录方式）
CREATE TABLE sys_account (
    id BIGINT NOT NULL COMMENT '账号ID（雪花ID）',
    user_id BIGINT NOT NULL COMMENT '关联用户ID',
    account_type TINYINT NOT NULL COMMENT '账号类型：1-用户名 2-手机号 3-邮箱 4-微信 5-APP',
    account_value VARCHAR(100) NOT NULL COMMENT '账号标识（用户名/手机号/邮箱/openid等）',
    password VARCHAR(100) COMMENT '密码（BCrypt加密，第三方登录为空）',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_value (account_type, account_value),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号表';

-- 角色表
CREATE TABLE sys_role (
    id BIGINT NOT NULL COMMENT '角色ID（雪花ID）',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码（如 ADMIN、APP_USER）',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称（如 管理员、小程序用户）',
    role_type TINYINT NOT NULL DEFAULT 1 COMMENT '角色类型：1-管理后台 2-小程序/APP 3-APP',
    description VARCHAR(200) COMMENT '角色描述',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 菜单/权限表
CREATE TABLE sys_menu (
    id BIGINT NOT NULL COMMENT '菜单ID（雪花ID）',
    parent_id BIGINT  COMMENT '父菜单ID（null表示顶级）',
    menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
    permission_code VARCHAR(100) COMMENT '权限标识（如 user:list、user:delete）',
    menu_type VARCHAR(20) NOT NULL COMMENT '菜单类型：menu-菜单 button-按钮',
    path VARCHAR(200) COMMENT '路由路径',
    icon VARCHAR(50) COMMENT '图标',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单/权限表';

-- 用户角色关联表
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色菜单关联表
CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 初始化数据

-- 默认角色
INSERT INTO sys_role (id, role_code, role_name, role_type, description) VALUES
(1, 'ADMIN', '超级管理员', 1, '管理后台超级管理员，拥有所有权限'),
(2, 'APP_USER', '小程序用户', 2, '小程序/APP端普通用户'),
(3, 'MOBILE_USER', 'APP用户', 3, 'APP端普通用户');

-- 默认菜单（管理后台基础菜单）
INSERT INTO sys_menu (id, parent_id, menu_name, permission_code, menu_type, path, icon, sort) VALUES
(1, 0, '系统管理', NULL, 'menu', '/system', 'setting', 1),
(2, 1, '用户管理', 'user:list', 'menu', '/system/user', 'user', 1),
(3, 2, '用户查询', 'user:query', 'button', NULL, NULL, 1),
(4, 2, '用户新增', 'user:add', 'button', NULL, NULL, 2),
(5, 2, '用户编辑', 'user:edit', 'button', NULL, NULL, 3),
(6, 2, '用户删除', 'user:delete', 'button', NULL, NULL, 4),
(7, 1, '角色管理', 'role:list', 'menu', '/system/role', 'peoples', 2),
(8, 1, '菜单管理', 'menu:list', 'menu', '/system/menu', 'tree-table', 3);

-- 管理员角色关联所有菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;