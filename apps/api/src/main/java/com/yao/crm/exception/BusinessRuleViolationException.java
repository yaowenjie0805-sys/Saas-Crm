package com.yao.crm.exception;

/**
 * 业务规则违反异常
 * 对应 HTTP 422 状态码
 */
public class BusinessRuleViolationException extends BusinessException {

    public BusinessRuleViolationException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION.getCode(), message);
    }

    public BusinessRuleViolationException(String message, String field) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION.getCode(), message, field);
    }

    public BusinessRuleViolationException(ErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }

    public BusinessRuleViolationException(ErrorCode errorCode, String message, String field) {
        super(errorCode.getCode(), message, field);
    }

    /**
     * 创建业务规则违反异常
     * @param ruleName 规则名称
     * @param reason 违反原因
     * @return BusinessRuleViolationException 实例
     */
    public static BusinessRuleViolationException of(String ruleName, String reason) {
        return new BusinessRuleViolationException(
            "Business rule violated: " + ruleName + " - " + reason
        );
    }

    /**
     * 使用指定错误码创建业务规则违反异常
     * @param errorCode 错误码枚举
     * @param message 错误消息
     * @return BusinessRuleViolationException 实例
     */
    public static BusinessRuleViolationException of(ErrorCode errorCode, String message) {
        return new BusinessRuleViolationException(errorCode, message);
    }
}
