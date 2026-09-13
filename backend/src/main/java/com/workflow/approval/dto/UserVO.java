package com.workflow.approval.dto;

import lombok.Data;

/**
 * 人员列表行（不含密码）
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String role;
    private Long deptId;
    private String deptName;
    private Integer status;
}
