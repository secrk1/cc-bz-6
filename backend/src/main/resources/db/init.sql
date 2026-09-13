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

-- 流程定义主表（设计器阶段仅维护草稿状态 DRAFT）
CREATE TABLE IF NOT EXISTS `wf_process_definition` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `process_name` VARCHAR(128)   NOT NULL COMMENT '流程名称',
    `process_key`  VARCHAR(64)    NOT NULL COMMENT '流程唯一标识',
    `status`       VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿，PUBLISHED-已发布',
    `remark`       VARCHAR(255)   DEFAULT NULL COMMENT '备注',
    `create_by`    BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人用户ID',
    `update_by`    BIGINT UNSIGNED DEFAULT NULL COMMENT '最后更新人用户ID',
    `create_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_process_key` (`process_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='流程定义主表';

-- 流程节点配置表（树形拓扑的扁平邻接表：审批 / 分叉 / 分支 / 汇聚）
-- 作用域 = (parent_id, branch_group)：
--   主链节点        parent_id=0, branch_group=NULL
--   分叉/汇聚行     parent_id=0 或外层分叉ID（嵌套时），branch_group=NULL
--   分支锚点行      parent_id=分叉ID, branch_group=组标识
--   分支内子节点    parent_id=分叉ID, branch_group=组标识，按 sort 构成该分支链路
CREATE TABLE IF NOT EXISTS `wf_process_node` (
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `process_id`         BIGINT UNSIGNED NOT NULL COMMENT '所属流程定义ID',
    `node_type`          VARCHAR(20)     NOT NULL DEFAULT 'APPROVAL' COMMENT '节点类型：APPROVAL-审批，GATEWAY_FORK-分叉，GATEWAY_BRANCH-分支锚点，GATEWAY_JOIN-汇聚',
    `gateway_mode`       VARCHAR(16)     DEFAULT NULL COMMENT '网关模式（仅分叉节点）：PARALLEL-并行，EXCLUSIVE-互斥',
    `join_node_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '分叉节点配对的汇聚节点ID（仅分叉行）',
    `parent_id`          BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父容器ID：0-主链，否则为分叉节点ID',
    `branch_group`       VARCHAR(40)     DEFAULT NULL COMMENT '分支组标识：同一次分叉的分支锚点与分支内子节点共享',
    `condition_expr`     VARCHAR(255)    DEFAULT NULL COMMENT '分支条件表达式（互斥网关用）',
    `node_name`          VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '节点名称（审批节点必填，分支行存条件名称）',
    `approver_type`      VARCHAR(32)     DEFAULT NULL COMMENT '审批人类别：USER-指定人员，DEPT_LEADER-部门负责人，ROLE_ADMIN-管理员（仅审批节点）',
    `approver_ref_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '审批对象引用ID（approverType=USER 时的用户ID）',
    `sort`               INT             NOT NULL DEFAULT 0 COMMENT '同作用域内的顺序，从0开始',
    `create_time`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_process_id` (`process_id`),
    KEY `idx_process_sort` (`process_id`, `parent_id`, `branch_group`, `sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='流程草稿节点配置表';

-- 初始部门、账号均由后端 DataInitializer 在应用启动时幂等创建：
--   内置部门：信息科技部 / 平台业务部 / 人力资源部 / 设计部
--   每个部门：1 名管理员（任部门负责人）+ 2 名普通用户，密码均为 12345678（BCrypt 存储）
