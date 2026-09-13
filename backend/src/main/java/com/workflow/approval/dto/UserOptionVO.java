package com.workflow.approval.dto;

import lombok.Data;

/**
 * 用户简要信息（下拉选项用）
 */
@Data
public class UserOptionVO {
    private Long id;
    private String username;
    private String nickname;
    private Long deptId;
    private String role;
}
