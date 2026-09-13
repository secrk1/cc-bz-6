package com.workflow.approval.dto;

import lombok.Data;

import java.util.List;

/**
 * 流程拓扑节点回显结构（递归树）：
 * type=APPROVAL 时为审批节点；type=GATEWAY 时携带 branches，
 * 分支内 nodes 继续是同结构节点。
 */
@Data
public class ProcessNodeVO {

    /** 行ID：审批节点为自身行ID，网关为分叉行ID */
    private Long id;

    /** APPROVAL / GATEWAY */
    private String type;

    private String nodeName;

    private String approverType;

    private Long approverRefId;

    /** approverType=USER 时的审批人姓名（昵称优先） */
    private String approverRefName;

    /** 网关模式：PARALLEL / EXCLUSIVE */
    private String gatewayMode;

    /** 网关配对的汇聚节点行ID */
    private Long joinId;

    /** 网关的分支（仅 type=GATEWAY） */
    private List<BranchVO> branches;
}
