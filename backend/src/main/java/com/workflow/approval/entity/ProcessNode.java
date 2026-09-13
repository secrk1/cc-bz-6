package com.workflow.approval.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程节点配置（扁平邻接表，由服务层解析为递归树）：
 * APPROVAL-审批节点；GATEWAY_FORK-分叉（携带分支列表）；
 * GATEWAY_BRANCH-分支锚点行（承载条件与排序）；GATEWAY_JOIN-分叉配对的汇聚。
 * 开始/结束为设计器内置节点，不落库。
 */
@Data
@TableName("wf_process_node")
public class ProcessNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long processId;

    /** APPROVAL / GATEWAY_FORK / GATEWAY_BRANCH / GATEWAY_JOIN */
    private String nodeType;

    /** 网关模式（仅分叉）：PARALLEL / EXCLUSIVE */
    private String gatewayMode;

    /** 配对汇聚节点ID（仅分叉行） */
    private Long joinNodeId;

    /** 父容器ID：0-主链，否则为分叉节点ID */
    private Long parentId;

    /** 分支组标识：同一次分叉的分支锚点与分支内子节点共享 */
    private String branchGroup;

    /** 分支条件表达式（互斥网关分支行） */
    private String conditionExpr;

    private String nodeName;

    /** 审批人类别：USER / DEPT_LEADER / ROLE_ADMIN（仅审批节点） */
    private String approverType;

    /** approverType=USER 时的用户ID */
    private Long approverRefId;

    /** 同作用域内的顺序，从 0 开始 */
    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
