package com.workflow.approval.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 流程画布草稿保存请求：上送流程主信息与开始/结束之间的完整审批链（全量覆盖）
 */
@Data
public class DraftSaveRequest {

    @NotBlank(message = "流程名称不能为空")
    @Size(max = 128, message = "流程名称最长 128 个字符")
    private String processName;

    @Size(max = 255, message = "备注最长 255 个字符")
    private String remark;

    /** 主链节点（审批节点 / 条件分支网关），网关分支内可继续递归嵌套节点 */
    @Valid
    private List<ProcessNodeSaveRequest> nodes;
}
