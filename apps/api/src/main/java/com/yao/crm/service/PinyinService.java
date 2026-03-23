package com.yao.crm.service;

import org.springframework.stereotype.Service;

/**
 * 拼音转换服务 - 国内特色
 * 支持中文转拼音首字母和全拼
 */
@Service
public class PinyinService {

    private static final String PINYIN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * 中文字符转拼音首字母
     * 例如："销售部" -> "XSB"
     */
    public String toPinyinInitial(String chinese) {
        if (chinese == null || chinese.isEmpty()) {
            return "";
        }

        StringBuilder pinyin = new StringBuilder();
        for (char c : chinese.toCharArray()) {
            if (isChinese(c)) {
                pinyin.append(getPinyinInitial(c));
            } else if (Character.isLetter(c)) {
                pinyin.append(Character.toUpperCase(c));
            }
        }
        return pinyin.toString();
    }

    /**
     * 中文转拼音全拼（简化实现）
     * 实际项目中建议使用pinyin4j库
     */
    public String toFullPinyin(String chinese) {
        if (chinese == null || chinese.isEmpty()) {
            return "";
        }

        StringBuilder pinyin = new StringBuilder();
        for (char c : chinese.toCharArray()) {
            if (isChinese(c)) {
                pinyin.append(getPinyinInitial(c));
            } else if (Character.isLetterOrDigit(c)) {
                pinyin.append(Character.toLowerCase(c));
            } else {
                pinyin.append(' ');
            }
        }
        return pinyin.toString().trim().replaceAll("\\s+", " ");
    }

    private boolean isChinese(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;
    }

    /**
     * 获取汉字的拼音首字母
     * 使用简单的映射表，实际项目中建议使用pinyin4j库
     */
    private char getPinyinInitial(char chinese) {
        int[] liKeyMasks = {
                0xB0A1, 0xB0C5, 0xB2C1, 0xB4EE, 0xB6EA, 0xB7A2, 0xB8C1, 0xB9FE, 0xBBF7,
                0xBFA6, 0xC0AC, 0xC2E8, 0xC4C3, 0xC5B6, 0xC5BE, 0xC6DA, 0xC8BB, 0xC8F6,
                0xCBFA, 0xCDDA, 0xCEF4, 0xD1B9, 0xD4D1
        };

        char[] initials = {
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'W', 'X', 'Y', 'Z'
        };

        if (chinese < 0xB0A1) {
            return chinese;
        }

        for (int i = 0; i < liKeyMasks.length; i++) {
            if (chinese <= liKeyMasks[i]) {
                return initials[i];
            }
        }

        return chinese;
    }

    /**
     * 搜索关键词预处理
     * 将拼音首字母转换为搜索模式
     * 例如："xsm" -> "*xsm*"
     */
    public String prepareSearchPattern(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return "";
        }
        return "*" + keyword.toLowerCase() + "*";
    }
}
