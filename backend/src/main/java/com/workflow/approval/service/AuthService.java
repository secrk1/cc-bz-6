package com.workflow.approval.service;

import com.workflow.approval.dto.LoginRequest;
import com.workflow.approval.dto.LoginResponse;
import com.workflow.approval.dto.UserInfoVO;

public interface AuthService {

    /** 账号密码登录，校验通过后签发 JWT */
    LoginResponse login(LoginRequest request);

    /** 根据用户ID查询用户信息（令牌校验接口使用） */
    UserInfoVO getUserInfo(Long userId);
}
