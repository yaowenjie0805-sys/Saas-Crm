package com.yao.crm.exception;

/**
 * 错误码枚举
 * 统一定义系统中所有业务错误码
 */
public enum ErrorCode {

    // ============ 通用错误码 ============
    VALIDATION_ERROR("VALIDATION_ERROR"),
    ENTITY_NOT_FOUND("ENTITY_NOT_FOUND"),
    DUPLICATE_ENTITY("DUPLICATE_ENTITY"),
    ACCESS_DENIED("ACCESS_DENIED"),
    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION"),
    INTERNAL_ERROR("INTERNAL_ERROR"),

    // ============ 认证相关错误码 ============
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS"),
    AUTH_TOKEN_EXPIRED("AUTH_TOKEN_EXPIRED"),
    AUTH_TOKEN_INVALID("AUTH_TOKEN_INVALID"),

    // ============ 客户相关错误码 ============
    CUSTOMER_NOT_FOUND("CUSTOMER_NOT_FOUND"),
    CUSTOMER_DUPLICATE_EMAIL("CUSTOMER_DUPLICATE_EMAIL"),

    // ============ 订单相关错误码 ============
    ORDER_INVALID_STATUS("ORDER_INVALID_STATUS"),
    ORDER_NOT_FOUND("ORDER_NOT_FOUND"),

    // ============ 审批相关错误码 ============
    APPROVAL_ALREADY_PROCESSED("APPROVAL_ALREADY_PROCESSED"),
    APPROVAL_NOT_FOUND("APPROVAL_NOT_FOUND"),

    // ============ 工作流相关错误码 ============
    WORKFLOW_INVALID_TRANSITION("WORKFLOW_INVALID_TRANSITION"),
    WORKFLOW_NOT_FOUND("WORKFLOW_NOT_FOUND");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }
}
