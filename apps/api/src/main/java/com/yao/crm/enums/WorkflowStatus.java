package com.yao.crm.enums;

/**
 * 工作流状态枚举
 */
public enum WorkflowStatus {
    DRAFT,       // 草稿
    ACTIVE,      // 活跃
    PAUSED,      // 暂停
    RUNNING,     // 运行中
    COMPLETED,   // 已完成
    FAILED,      // 失败
    CANCELLED;   // 已取消

    public static boolean isDraft(String status) {
        return DRAFT.name().equals(status);
    }

    public static boolean isActive(String status) {
        return ACTIVE.name().equals(status);
    }

    public static boolean isPaused(String status) {
        return PAUSED.name().equals(status);
    }

    public static boolean isRunning(String status) {
        return RUNNING.name().equals(status);
    }

    public static boolean isCompleted(String status) {
        return COMPLETED.name().equals(status);
    }

    public static boolean isFailed(String status) {
        return FAILED.name().equals(status);
    }

    public static boolean isCancelled(String status) {
        return CANCELLED.name().equals(status);
    }
}
