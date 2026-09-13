package com.workflow.approval.common;

import lombok.Getter;

/**
 * 统一业务返回码
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或认证已失效"),
    FORBIDDEN(403, "没有访问权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),

    USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误"),
    ACCOUNT_DISABLED(1002, "账号已被禁用"),
    USER_NOT_FOUND(1003, "用户不存在"),
    USERNAME_DUPLICATE(1004, "用户名已存在"),
    USER_IS_DEPT_LEADER(1005, "该用户是部门负责人，请先在部门管理中更换负责人后再操作"),
    CANNOT_DELETE_SELF(1006, "不能删除当前登录账号"),

    DEPT_NOT_FOUND(2001, "部门不存在"),
    PARENT_DEPT_NOT_FOUND(2002, "上级部门不存在"),
    DEPT_NAME_DUPLICATE(2003, "同一上级部门下已存在同名部门"),
    DEPT_HAS_CHILDREN(2004, "该部门下存在子部门，不允许删除"),
    DEPT_HAS_USERS(2005, "该部门下仍有用户，不允许删除"),
    DEPT_PARENT_INVALID(2006, "不能将部门挂载到自身或其子部门下"),
    LEADER_BELONGS_OTHER_DEPT(2007, "该用户已归属其他部门，不能设置为本部门负责人");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
