package com.yao.crm.util;

/**
 * 字符串工具类
 */
public class StringUtils {

    /**
     * 检查字符串是否为空或仅包含空白字符
     */
    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 规范化字符串（去除首尾空白，null转为空字符串）
     */
    public static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
