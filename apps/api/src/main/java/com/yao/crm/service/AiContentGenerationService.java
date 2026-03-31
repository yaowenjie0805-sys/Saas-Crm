package com.yao.crm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * AI 内容生成服务
 */
@Service
@Slf4j
public class AiContentGenerationService {

    @Autowired
    private AiService aiService;

    @Autowired(required = false)
    private CacheService cacheService;

    /**
     * 生成跟进记录摘要
     */
    public String generateFollowUpSummary(String customerName, String interactionDetails, String channel) {
        String prompt = String.format(
            "请为以下客户跟进记录生成简洁的摘要（50字以内）：\n" +
            "客户：%s\n" +
            "渠道：%s\n" +
            "详情：%s",
            customerName, channel, interactionDetails
        );
        return aiService.generateText(prompt, Map.of("temperature", 0.5, "max_tokens", 100));
    }

    /**
     * 生成评论回复建议
     */
    public String generateCommentReply(String originalComment, String context) {
        String prompt = String.format(
            "作为客服，请为以下评论生成一条专业的回复建议（30字以内）：\n" +
            "原评论：%s\n" +
            "背景：%s",
            originalComment, context
        );
        return aiService.generateText(prompt, Map.of("temperature", 0.7, "max_tokens", 50));
    }

    /**
     * 生成营销邮件模板
     */
    public String generateMarketingEmail(String customerName, String productName, String customerInterest) {
        String prompt = String.format(
            "为以下客户生成一封个性化的营销邮件（100字以内）：\n" +
            "客户：%s\n" +
            "产品：%s\n" +
            "客户兴趣：%s",
            customerName, productName, customerInterest
        );
        return aiService.generateText(prompt, Map.of("temperature", 0.8, "max_tokens", 200));
    }

    /**
     * 检查服务是否可用
     */
    public boolean isAvailable() {
        return aiService != null && aiService.isAvailable();
    }
}