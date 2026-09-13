package com.workflow.approval.dto;

import lombok.Data;

/**
 * 用户基本信息（返回给前端，不含密码）
 */
@Data
public class UserInfoVO {

    private Long id;
    private Long deptId;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private String role;
}
