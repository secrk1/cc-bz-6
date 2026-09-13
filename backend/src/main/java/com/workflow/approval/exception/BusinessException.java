package com.workflow.approval.exception;

import com.workflow.approval.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常：由 GlobalExceptionHandler 统一转换为标准返回结构
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
