package com.yao.crm.service;

import java.util.Map;

/**
 * AI 服务接口
 */
public interface AiService {

    /**
     * 生成文本内容
     */
    String generateText(String prompt);

    /**
     * 生成文本内容（带参数）
     */
    String generateText(String prompt, Map<String, Object> options);

    /**
     * 检查服务是否可用
     */
    boolean isAvailable();
}
