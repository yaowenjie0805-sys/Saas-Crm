package com.yao.crm.exception;

/**
 * 校验异常
 * 对应 HTTP 400 状态码
 */
public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR.getCode(), message);
    }

    public ValidationException(String message, String field) {
        super(ErrorCode.VALIDATION_ERROR.getCode(), message, field);
    }

    public ValidationException(ErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }

    public ValidationException(ErrorCode errorCode, String message, String field) {
        super(errorCode.getCode(), message, field);
    }

    /**
     * 创建字段校验异常
     * @param field 字段名
     * @param message 错误消息
     * @return ValidationException 实例
     */
    public static ValidationException forField(String field, String message) {
        return new ValidationException(message, field);
    }

    /**
     * 创建指定错误码的字段校验异常
     * @param errorCode 错误码枚举
     * @param field 字段名
     * @param message 错误消息
     * @return ValidationException 实例
     */
    public static ValidationException forField(ErrorCode errorCode, String field, String message) {
        return new ValidationException(errorCode, message, field);
    }
}
