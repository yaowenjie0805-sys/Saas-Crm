package com.yao.crm.exception;

/**
 * 实体重复异常
 * 对应 HTTP 409 状态码
 */
public class DuplicateEntityException extends BusinessException {

    public DuplicateEntityException(String message) {
        super(ErrorCode.DUPLICATE_ENTITY.getCode(), message);
    }

    public DuplicateEntityException(String message, String field) {
        super(ErrorCode.DUPLICATE_ENTITY.getCode(), message, field);
    }

    public DuplicateEntityException(ErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }

    public DuplicateEntityException(ErrorCode errorCode, String message, String field) {
        super(errorCode.getCode(), message, field);
    }

    /**
     * 创建字段重复异常
     * @param entityType 实体类型名称
     * @param field 字段名
     * @param value 字段值
     * @return DuplicateEntityException 实例
     */
    public static DuplicateEntityException forField(String entityType, String field, Object value) {
        return new DuplicateEntityException(
            entityType + " already exists with " + field + ": " + value,
            field
        );
    }

    /**
     * 使用指定错误码创建重复异常
     * @param errorCode 错误码枚举
     * @param field 字段名
     * @param value 字段值
     * @return DuplicateEntityException 实例
     */
    public static DuplicateEntityException of(ErrorCode errorCode, String field, Object value) {
        return new DuplicateEntityException(
            errorCode,
            "Duplicate value for " + field + ": " + value,
            field
        );
    }
}
