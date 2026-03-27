package com.yao.crm.enums;

/**
 * 通用实体状态枚举
 */
public enum EntityStatus {
    ACTIVE,      // 活跃
    INACTIVE,    // 非活跃
    PENDING,     // 待处理
    DRAFT,       // 草稿
    ARCHIVED;    // 已归档

    public static boolean isActive(String status) {
        return ACTIVE.name().equals(status);
    }

    public static boolean isInactive(String status) {
        return INACTIVE.name().equals(status);
    }

    public static boolean isPending(String status) {
        return PENDING.name().equals(status);
    }

    public static boolean isDraft(String status) {
        return DRAFT.name().equals(status);
    }

    public static boolean isArchived(String status) {
        return ARCHIVED.name().equals(status);
    }
}
