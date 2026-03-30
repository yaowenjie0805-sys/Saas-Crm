package com.yao.crm.service;

import com.yao.crm.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表工具类 - 提供报表服务中通用的静态工具方法
 */
public final class ReportUtils {

    private static final LocalDate OPEN_RANGE_START = LocalDate.of(1970, 1, 1);
    private static final LocalDate OPEN_RANGE_END = LocalDate.of(2099, 12, 31);

    private ReportUtils() {
        // 工具类禁止实例化
    }

    /**
     * 规范化起始日期
     */
    public static LocalDate normalizeFromDate(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null) {
            return fromDate;
        }
        if (toDate != null) {
            return OPEN_RANGE_START;
        }
        return null;
    }

    /**
     * 规范化结束日期
     */
    public static LocalDate normalizeToDate(LocalDate fromDate, LocalDate toDate) {
        if (toDate != null) {
            return toDate;
        }
        if (fromDate != null) {
            return OPEN_RANGE_END;
        }
        return null;
    }

    /**
     * 规范化角色字符串（转为大写）
     */
    public static String normalizeRole(String role) {
        return normalizeCacheValue(role).toUpperCase();
    }

    /**
     * 规范化字符串（转为小写，去除空白）
     */
    public static String normalized(String text) {
        return StringUtils.isBlank(text) ? "" : text.trim().toLowerCase();
    }

    /**
     * 检查字符串是否为空或仅包含空白字符
     */
    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 规范化缓存值（去除首尾空白）
     */
    public static String normalizeCacheValue(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 规范化日期为字符串
     */
    public static String normalizeDate(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    /**
     * 安全获取 Long 值，null 时返回 0
     */
    public static long safeLong(Long value) {
        return value == null ? 0L : value.longValue();
    }

    /**
     * 将 Object[] 列表转换为 Map<String, Integer>
     */
    public static Map<String, Integer> toIntMap(List<Object[]> rows) {
        Map<String, Integer> out = new HashMap<String, Integer>();
        if (rows == null) {
            return out;
        }
        for (Object[] row : rows) {
            String key = normalizeBucket(row != null && row.length > 0 ? row[0] : null);
            int value = asInt(row != null && row.length > 1 ? row[1] : null);
            out.put(key, value);
        }
        return out;
    }

    /**
     * 将 Object[] 列表转换为 Map<String, Long>
     */
    public static Map<String, Long> toLongMap(List<Object[]> rows) {
        Map<String, Long> out = new HashMap<String, Long>();
        if (rows == null) {
            return out;
        }
        for (Object[] row : rows) {
            String key = normalizeBucket(row != null && row.length > 0 ? row[0] : null);
            long value = asLong(row != null && row.length > 1 ? row[1] : null);
            out.put(key, value);
        }
        return out;
    }

    /**
     * 规范化分组键值
     */
    public static String normalizeBucket(Object keyRaw) {
        String key = keyRaw == null ? "" : String.valueOf(keyRaw);
        return isBlank(key) ? "Unknown" : key;
    }

    /**
     * 将对象转换为 int
     */
    public static int asInt(Object value) {
        return (int) asLong(value);
    }

    /**
     * 将对象转换为 long
     */
    public static long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return 0L;
        }
    }

    /**
     * 获取日期的开始时间
     */
    public static LocalDateTime startOfDay(LocalDate value) {
        return value == null ? null : value.atStartOfDay();
    }

    /**
     * 获取日期的结束时间
     */
    public static LocalDateTime endOfDay(LocalDate value) {
        return value == null ? null : value.plusDays(1).atStartOfDay().minusNanos(1);
    }

    /**
     * 安全地将对象转换为 Map
     */
    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> castMap(Object obj) {
        if (!(obj instanceof Map)) {
            return new HashMap<String, T>();
        }
        return (Map<String, T>) obj;
    }
}
