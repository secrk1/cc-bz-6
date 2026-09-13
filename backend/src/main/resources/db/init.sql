-- 综合工作流审批系统 初始化脚本
-- 由 MySQL 容器首次启动时自动执行（/docker-entrypoint-initdb.d）

CREATE DATABASE IF NOT EXISTS `workflow_approval`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `workflow_approval`;

-- 部门表（邻接表模型：parent_id=0 表示根部门）
CREATE TABLE IF NOT EXISTS `sys_dept` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父部门ID，0表示根部门',
    `dept_name`   VARCHAR(64)     NOT NULL COMMENT '部门名称（同一父级下唯一）',
    `leader_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '部门负责人用户ID',
    `sort`        INT             NOT NULL DEFAULT 0 COMMENT '显示排序，数字越小越靠前',
    `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `remark`      VARCHAR(255)    DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_parent_name` (`parent_id`, `dept_name`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='部门表';

-- 系统用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dept_id`     BIGINT UNSIGNED DEFAULT NULL COMMENT '所属部门ID',
    `username`    VARCHAR(64)     NOT NULL COMMENT '登录账号',
    `password`    VARCHAR(100)    NOT NULL COMMENT 'BCrypt 密码哈希',
    `nickname`    VARCHAR(64)     DEFAULT NULL COMMENT '昵称',
    `email`       VARCHAR(128)    DEFAULT NULL COMMENT '邮箱',
    `phone`       VARCHAR(32)     DEFAULT NULL COMMENT '手机号',
    `avatar`      VARCHAR(255)    DEFAULT NULL COMMENT '头像地址',
    `role`        VARCHAR(32)     NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN-管理员，USER-普通用户',
    `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_dept_id` (`dept_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统用户表';

-- 初始部门、账号均由后端 DataInitializer 在应用启动时幂等创建：
--   内置部门：信息科技部 / 平台业务部 / 人力资源部 / 设计部
--   每个部门：1 名管理员（任部门负责人）+ 2 名普通用户，密码均为 12345678（BCrypt 存储）
