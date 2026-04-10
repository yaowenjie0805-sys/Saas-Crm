package com.yao.crm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class AiContentGenerationService {

    @Autowired
    private AiService aiService;

    @Autowired(required = false)
    private CacheService cacheService;

    public String generateFollowUpSummary(String customerName, String interactionDetails, String channel) {
        return generateFollowUpSummary(customerName, interactionDetails, channel, null);
    }

    public String generateFollowUpSummary(String customerName, String interactionDetails, String channel, String model) {
        return generateFollowUpSummary(customerName, interactionDetails, channel, model, null, null);
    }

    public String generateFollowUpSummary(
            String customerName,
            String interactionDetails,
            String channel,
            String model,
            String baseUrl,
            String apiKey
    ) {
        String prompt = String.format(
                "请为以下客户跟进记录生成简洁的摘要（50字以内）：%n客户：%s%n渠道：%s%n详情：%s",
                customerName,
                channel,
                interactionDetails
        );
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("temperature", 0.5);
        options.put("max_tokens", 100);
        if (model != null && !model.trim().isEmpty()) {
            options.put("model", model.trim());
        }
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            options.put("base_url", baseUrl.trim());
        }
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            options.put("api_key", apiKey.trim());
        }
        return aiService.generateText(prompt, options);
    }

    public String generateCommentReply(String originalComment, String context) {
        return generateCommentReply(originalComment, context, null, null, null);
    }

    public String generateCommentReply(
            String originalComment,
            String context,
            String model,
            String baseUrl,
            String apiKey
    ) {
        String prompt = String.format(
                "作为客服，请为以下评论生成一条专业的回复建议（50字以内）：%n原评论：%s%n背景：%s",
                originalComment,
                context
        );
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("temperature", 0.7);
        options.put("max_tokens", 50);
        if (model != null && !model.trim().isEmpty()) {
            options.put("model", model.trim());
        }
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            options.put("base_url", baseUrl.trim());
        }
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            options.put("api_key", apiKey.trim());
        }
        return aiService.generateText(prompt, options);
    }

    public String generateMarketingEmail(String customerName, String productName, String customerInterest) {
        return generateMarketingEmail(customerName, productName, customerInterest, null, null, null);
    }

    public String generateMarketingEmail(
            String customerName,
            String productName,
            String customerInterest,
            String model,
            String baseUrl,
            String apiKey
    ) {
        String prompt = String.format(
                "为以下客户生成一封个性化的营销邮件（200字以内）：%n客户：%s%n产品：%s%n客户兴趣：%s",
                customerName,
                productName,
                customerInterest
        );
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("temperature", 0.8);
        options.put("max_tokens", 200);
        if (model != null && !model.trim().isEmpty()) {
            options.put("model", model.trim());
        }
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            options.put("base_url", baseUrl.trim());
        }
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            options.put("api_key", apiKey.trim());
        }
        return aiService.generateText(prompt, options);
    }

    public boolean isAvailable() {
        return aiService != null && aiService.isAvailable();
    }
}
