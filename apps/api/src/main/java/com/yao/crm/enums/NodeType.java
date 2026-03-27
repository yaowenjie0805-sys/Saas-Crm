package com.yao.crm.enums;

/**
 * 工作流节点类型枚举
 */
public enum NodeType {
    TRIGGER,
    CONDITION,
    ACTION,
    NOTIFICATION,
    APPROVAL,
    WAIT,
    CC,
    END;
    
    /**
     * 从字符串转换为枚举
     * @param value 字符串值
     * @return 对应的枚举值，如果无法识别则返回 null
     */
    public static NodeType fromString(String value) {
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
