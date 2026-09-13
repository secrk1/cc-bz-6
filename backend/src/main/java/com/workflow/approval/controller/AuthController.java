package com.workflow.approval.controller;

import com.workflow.approval.common.Result;
import com.workflow.approval.dto.LoginRequest;
import com.workflow.approval.dto.LoginResponse;
import com.workflow.approval.dto.UserInfoVO;
import com.workflow.approval.security.LoginUser;
import com.workflow.approval.security.UserContext;
import com.workflow.approval.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 账号密码登录（无需令牌）
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 令牌校验 / 获取当前登录用户信息（需携带 Bearer Token）
     */
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        LoginUser loginUser = UserContext.get();
        return Result.success(authService.getUserInfo(loginUser.getUserId()));
    }

    /**
     * 登出：JWT 为无状态令牌，服务端仅返回成功；前端删除本地令牌即可。
     * 后续如需服务端踢人，可在此引入黑名单/Redis。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
