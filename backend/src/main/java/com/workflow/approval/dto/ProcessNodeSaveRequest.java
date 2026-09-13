package com.workflow.approval.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 流程拓扑中的一个节点（递归树结构）：
 * type=APPROVAL 时为审批节点；type=GATEWAY 时携带 branches（分支），
 * 每个分支内部又是一串节点，可继续嵌套网关。
 */
@Data
public class ProcessNodeSaveRequest {

    /** 已保存节点的行ID（分叉节点为分叉行ID）；画布新增的节点为空 */
    private Long id;

    /** APPROVAL-审批节点 GATEWAY-条件分支网关 */
    private String type;

    @Size(max = 64, message = "节点名称最长 64 个字符")
    private String nodeName;

    /** 审批人类别：USER / DEPT_LEADER / ROLE_ADMIN */
    private String approverType;

    /** approverType=USER 时必填：指定的用户ID */
    private Long approverRefId;

    /** 网关模式（type=GATEWAY）：PARALLEL-并行 EXCLUSIVE-互斥 */
    private String gatewayMode;

    /** 网关分支（type=GATEWAY 时必填且至少 2 条） */
    @Valid
    private List<BranchSaveRequest> branches;
}
