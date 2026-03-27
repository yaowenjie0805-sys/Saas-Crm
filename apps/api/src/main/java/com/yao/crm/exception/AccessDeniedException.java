package com.yao.crm.exception;

/**
 * 访问拒绝异常
 * 对应 HTTP 403 状态码
 */
public class AccessDeniedException extends BusinessException {

    public AccessDeniedException(String message) {
        super(ErrorCode.ACCESS_DENIED.getCode(), message);
    }

    public AccessDeniedException(String message, String field) {
        super(ErrorCode.ACCESS_DENIED.getCode(), message, field);
    }

    public AccessDeniedException(ErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }

    public AccessDeniedException(ErrorCode errorCode, String message, String field) {
        super(errorCode.getCode(), message, field);
    }

    /**
     * 创建资源访问拒绝异常
     * @param resource 资源名称
     * @param action 操作名称
     * @return AccessDeniedException 实例
     */
    public static AccessDeniedException forResource(String resource, String action) {
        return new AccessDeniedException("Access denied for " + action + " on " + resource);
    }

    /**
     * 创建操作权限不足异常
     * @param operation 操作名称
     * @return AccessDeniedException 实例
     */
    public static AccessDeniedException forOperation(String operation) {
        return new AccessDeniedException("You don't have permission to " + operation);
    }
}
