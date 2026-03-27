package com.yao.crm.enums;

/**
 * 条件操作符枚举
 */
public enum ConditionOperator {
    IS_NULL,
    IS_EMPTY,
    IS_NOT_NULL,
    IS_NOT_EMPTY,
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    GREATER_THAN,
    LESS_THAN,
    GREATER_EQUAL,
    LESS_EQUAL,
    IN,
    NOT_IN,
    BETWEEN,
    REGEX;
    
    /**
     * 从字符串转换为枚举
     * @param value 字符串值
     * @return 对应的枚举值，如果无法识别则返回 null
     */
    public static ConditionOperator fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
