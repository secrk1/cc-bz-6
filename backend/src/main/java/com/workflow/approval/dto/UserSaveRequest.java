package com.workflow.approval.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 人员新增/编辑请求
 */
@Data
public class UserSaveRequest {

    /** 编辑时必填，新增时为空 */
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 64, message = "用户名长度需在 3-64 个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /**
     * 登录密码：新增必填，编辑时留空表示不修改
     */
    @Size(min = 6, max = 64, message = "密码长度需在 6-64 个字符之间")
    private String password;

    @Size(max = 64, message = "昵称最长 64 个字符")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱最长 128 个字符")
    private String email;

    /** ADMIN-管理员，USER-员工 */
    @NotBlank(message = "请选择角色")
    @Pattern(regexp = "ADMIN|USER", message = "角色取值非法")
    private String role;

    /** 所属部门，可空（未分配部门） */
    private Long deptId;

    @NotNull(message = "请选择状态")
    private Integer status;
}
