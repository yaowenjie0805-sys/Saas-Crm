package com.yao.crm.enums;

import java.util.Set;

/**
 * 订单状态枚举
 */
public enum OrderStatus {
    DRAFT("DRAFT"),          // 草稿
    CONFIRMED("CONFIRMED"),  // 已确认
    FULFILLING("FULFILLING"), // 执行中
    COMPLETED("COMPLETED"),  // 已完成
    CANCELED("CANCELED");    // 已取消

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * 从字符串值获取枚举实例
     */
    public static OrderStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (OrderStatus status : OrderStatus.values()) {
            if (status.value.equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        return null;
    }

    /**
     * 检查是否为有效状态
     */
    public static boolean isValid(String value) {
        return fromValue(value) != null;
    }

    /**
     * 获取所有有效状态值
     */
    public static Set<String> getValidValues() {
        return Set.of(DRAFT.value, CONFIRMED.value, FULFILLING.value, COMPLETED.value, CANCELED.value);
    }

    /**
     * 检查是否可取消的状态
     */
    public static boolean isCancelable(String status) {
        String upper = status != null ? status.toUpperCase() : "";
        return DRAFT.value.equals(upper)
            || CONFIRMED.value.equals(upper)
            || FULFILLING.value.equals(upper);
    }

    /**
     * 检查是否为终态（不可转换）
     */
    public static boolean isFinal(String status) {
        String upper = status != null ? status.toUpperCase() : "";
        return COMPLETED.value.equals(upper) || CANCELED.value.equals(upper);
    }

    /**
     * 检查状态是否支持转换到合同
     */
    public static boolean canTransitionToContract(String status) {
        String upper = status != null ? status.toUpperCase() : "";
        return CONFIRMED.value.equals(upper) || FULFILLING.value.equals(upper);
    }
}
