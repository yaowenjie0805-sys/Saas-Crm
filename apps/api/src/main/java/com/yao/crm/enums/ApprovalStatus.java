package com.yao.crm.enums;

/**
 * 审批状态枚举
 */
public enum ApprovalStatus {
    PENDING,     // 待审批
    APPROVED,    // 已批准
    REJECTED,    // 已拒绝
    ESCALATED,   // 已升级
    WAITING,     // 等待中
    COMPLETED,   // 已完成
    CANCELLED;   // 已取消

    public static boolean isPending(String status) {
        return PENDING.name().equals(status);
    }

    public static boolean isApproved(String status) {
        return APPROVED.name().equals(status);
    }

    public static boolean isRejected(String status) {
        return REJECTED.name().equals(status);
    }

    public static boolean isEscalated(String status) {
        return ESCALATED.name().equals(status);
    }

    public static boolean isWaiting(String status) {
        return WAITING.name().equals(status);
    }

    public static boolean isCompleted(String status) {
        return COMPLETED.name().equals(status);
    }

    public static boolean isCancelled(String status) {
        return CANCELLED.name().equals(status);
    }
}
