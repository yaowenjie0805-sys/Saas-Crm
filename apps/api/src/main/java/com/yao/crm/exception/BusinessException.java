package com.yao.crm.exception;

/**
 * 业务异常基类
 * 所有业务相关的异常都应继承此类
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final String field;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.field = null;
    }

    public BusinessException(String errorCode, String message, String field) {
        super(message);
        this.errorCode = errorCode;
        this.field = field;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.field = null;
    }

    public BusinessException(String errorCode, String message, String field, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.field = field;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getField() {
        return field;
    }

    public boolean hasField() {
        return field != null && !field.isEmpty();
    }
}
