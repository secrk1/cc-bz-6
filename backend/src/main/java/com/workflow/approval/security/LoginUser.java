package com.workflow.approval.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 从令牌中解析出的登录用户上下文
 */
@Data
@AllArgsConstructor
public class LoginUser {
    private Long userId;
    private String username;
    private String role;
}
