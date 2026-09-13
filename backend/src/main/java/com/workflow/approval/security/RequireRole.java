package com.workflow.approval.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色限制：当前登录用户必须具备指定角色之一。
 * 标注在 Controller 方法或类上，由 {@code RoleAspect} 校验。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /** 允许访问的角色，如 {"ADMIN"} */
    String[] value();
}
