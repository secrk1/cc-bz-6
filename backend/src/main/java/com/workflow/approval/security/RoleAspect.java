package com.workflow.approval.security;

import com.workflow.approval.common.ResultCode;
import com.workflow.approval.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 角色权限切面：拦截标注了 {@link RequireRole} 的方法/类，
 * 未登录或角色不匹配时返回 403。
 */
@Slf4j
@Aspect
@Component
public class RoleAspect {

    @Before("@within(com.workflow.approval.security.RequireRole) || " +
            "@annotation(com.workflow.approval.security.RequireRole)")
    public void checkRole(JoinPoint point) {
        LoginUser user = UserContext.get();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        RequireRole annotation = resolveAnnotation(point);
        if (annotation == null) {
            return;
        }

        String[] allowedRoles = annotation.value();
        boolean allowed = user.getRole() != null
                && Arrays.asList(allowedRoles).contains(user.getRole());
        if (!allowed) {
            log.warn("用户[{}]角色[{}]越权访问受保护接口[{}]",
                    user.getUsername(), user.getRole(), point.getSignature().toShortString());
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private RequireRole resolveAnnotation(JoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        // 方法级注解优先
        RequireRole methodAnnotation = signature.getMethod().getAnnotation(RequireRole.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        // 回退到类级注解
        return point.getTarget().getClass().getAnnotation(RequireRole.class);
    }
}
