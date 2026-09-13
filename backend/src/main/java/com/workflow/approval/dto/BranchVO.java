package com.workflow.approval.dto;

import lombok.Data;

import java.util.List;

/**
 * 网关分支回显结构
 */
@Data
public class BranchVO {

    /** 分支锚点行ID（前端渲染 key 用） */
    private Long branchId;

    /** 分支/条件名称 */
    private String conditionName;

    /** 条件表达式 */
    private String conditionExpr;

    /** 分支内子节点链（已按 sort 升序） */
    private List<ProcessNodeVO> nodes;
}
