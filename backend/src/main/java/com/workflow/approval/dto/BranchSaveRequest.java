package com.workflow.approval.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 网关的一条分支：互斥网关需填写条件名称/表达式，并行网关为分支名称。
 * nodes 为该分支内按顺序排列的子节点链（可继续嵌套网关）。
 */
@Data
public class BranchSaveRequest {

    /** 分支/条件名称 */
    @Size(max = 64, message = "分支条件名称最长 64 个字符")
    private String conditionName;

    /** 条件表达式（互斥网关用，如 amount > 1000） */
    @Size(max = 255, message = "条件表达式最长 255 个字符")
    private String conditionExpr;

    @Valid
    private List<ProcessNodeSaveRequest> nodes;
}
