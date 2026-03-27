package com.yao.crm.enums;

/**
 * 用户角色枚举
 */
public enum UserRole {
    ADMIN,
    MANAGER,
    SALES,
    ANALYST,
    USER;

    public static boolean isAdmin(String role) {
        return ADMIN.name().equals(role);
    }

    public static boolean isManager(String role) {
        return MANAGER.name().equals(role);
    }

    public static boolean isSales(String role) {
        return SALES.name().equals(role);
    }

    public static boolean isAnalyst(String role) {
        return ANALYST.name().equals(role);
    }
}
