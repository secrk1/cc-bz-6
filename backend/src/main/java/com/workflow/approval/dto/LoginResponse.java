package com.workflow.approval.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录成功返回：认证令牌 + 用户基本信息
 */
@Data
@Builder
public class LoginResponse {

    private String token;
    private String tokenType;
    private long expiresIn;
    private UserInfoVO user;
}
