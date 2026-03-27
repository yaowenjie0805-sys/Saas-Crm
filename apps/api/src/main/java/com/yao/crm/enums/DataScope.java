package com.yao.crm.enums;

/**
 * 数据范围枚举
 */
public enum DataScope {
    SELF,      // 仅自己
    TEAM,      // 团队
    DEPT,      // 部门
    ALL;       // 全部

    public static boolean isSelf(String scope) {
        return SELF.name().equals(scope);
    }

    public static boolean isTeam(String scope) {
        return TEAM.name().equals(scope);
    }

    public static boolean isDept(String scope) {
        return DEPT.name().equals(scope);
    }

    public static boolean isAll(String scope) {
        return ALL.name().equals(scope);
    }
}
