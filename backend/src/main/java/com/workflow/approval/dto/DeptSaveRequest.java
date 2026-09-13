package com.workflow.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 部门新增/编辑请求
 */
@Data
public class DeptSaveRequest {

    /** 编辑时必填，新增时为空 */
    private Long id;

    /** 父部门ID，0 表示根部门 */
    @NotNull(message = "上级部门不能为空")
    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 64, message = "部门名称最长 64 个字符")
    private String deptName;

    /** 部门负责人用户ID，可为空 */
    private Long leaderId;

    private Integer sort = 0;

    private Integer status = 1;

    @Size(max = 255, message = "备注最长 255 个字符")
    private String remark;
}
