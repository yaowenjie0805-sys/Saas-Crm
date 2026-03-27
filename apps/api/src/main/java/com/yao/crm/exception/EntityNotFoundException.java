package com.yao.crm.exception;

/**
 * 实体未找到异常
 * 对应 HTTP 404 状态码
 */
public class EntityNotFoundException extends BusinessException {

    public EntityNotFoundException(String message) {
        super(ErrorCode.ENTITY_NOT_FOUND.getCode(), message);
    }

    public EntityNotFoundException(String message, String field) {
        super(ErrorCode.ENTITY_NOT_FOUND.getCode(), message, field);
    }

    public EntityNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }

    public EntityNotFoundException(ErrorCode errorCode, String message, String field) {
        super(errorCode.getCode(), message, field);
    }

    /**
     * 根据实体类型和ID创建异常
     * @param entityType 实体类型名称
     * @param id 实体ID
     * @return EntityNotFoundException 实例
     */
    public static EntityNotFoundException of(String entityType, Object id) {
        return new EntityNotFoundException(
            ErrorCode.ENTITY_NOT_FOUND.getCode(),
            entityType + " not found with id: " + id
        );
    }

    /**
     * 根据ErrorCode创建异常
     * @param errorCode 错误码枚举
     * @param id 实体ID
     * @return EntityNotFoundException 实例
     */
    public static EntityNotFoundException of(ErrorCode errorCode, Object id) {
        return new EntityNotFoundException(errorCode, errorCode.getCode().replace("_NOT_FOUND", "").toLowerCase() + " not found with id: " + id);
    }
}
