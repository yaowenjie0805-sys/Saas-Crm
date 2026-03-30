package com.yao.crm.enums;

/**
 * 线索状态枚举
 */
public enum LeadStatusEnum {
    NEW("NEW", "新建"),
    QUALIFIED("QUALIFIED", "已限定"),
    NURTURING("NURTURING", "培育中"),
    CONVERTED("CONVERTED", "已转换"),
    DISQUALIFIED("DISQUALIFIED", "已不合格");

    private final String value;
    private final String description;

    LeadStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static boolean isValid(String status) {
        if (status == null) {
            return false;
        }
        for (LeadStatusEnum enumValue : LeadStatusEnum.values()) {
            if (enumValue.value.equals(status)) {
                return true;
            }
        }
        return false;
    }
}
