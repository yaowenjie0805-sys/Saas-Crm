package com.yao.crm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * AI 销售预测增强服务
 */
@Service
public class AiSalesForecastService {
    private static final Logger log = LoggerFactory.getLogger(AiSalesForecastService.class);

    @Autowired
    private AiService aiService;

    /**
     * 计算商机赢单概率（AI 增强）
     */
    public double calculateWinProbability(String opportunityName, String stage,
            BigDecimal amount, int daysInStage, String competitorInfo) {

        if (!aiService.isAvailable()) {
            // AI 不可用时使用简单的加权模型
            return calculateSimpleWinProbability(stage, amount);
        }

        String prompt = String.format(
            "分析以下商机的赢单概率（返回 0-100 的数字）：\n" +
            "商机名称：%s\n" +
            "阶段：%s\n" +
            "金额：%s\n" +
            "在当前阶段天数：%d\n" +
            "竞争对手：%s",
            opportunityName, stage, amount, daysInStage,
            competitorInfo != null ? competitorInfo : "无"
        );

        try {
            String result = aiService.generateText(prompt, Map.of("temperature", 0.3));
            return parseProbability(result);
        } catch (Exception e) {
            log.error("Failed to get AI win probability", e);
            return calculateSimpleWinProbability(stage, amount);
        }
    }

    /**
     * 简单的赢单概率计算（降级方案）
     */
    private double calculateSimpleWinProbability(String stage, BigDecimal amount) {
        // 简化实现，可复用现有的阶段权重
        double stageWeight = switch (stage) {
            case "NEGOTIATION" -> 0.5;
            case "PROPOSAL" -> 0.3;
            case "QUALIFIED" -> 0.15;
            case "LEAD" -> 0.05;
            default -> 0.0;
        };

        // 大额订单概率调低（风险规避）
        if (amount != null && amount.compareTo(new BigDecimal("1000000")) > 0) {
            stageWeight *= 0.8;
        }

        return stageWeight;
    }

    private double parseProbability(String aiResponse) {
        try {
            // 简单解析数字
            String numStr = aiResponse.replaceAll("[^0-9.]", "");
            double value = Double.parseDouble(numStr);
            return Math.min(100, Math.max(0, value));
        } catch (Exception e) {
            log.warn("Failed to parse AI response: {}", aiResponse);
            return 50.0; // 默认50%
        }
    }

    /**
     * 生成销售建议
     */
    public String generateSalesAdvice(String opportunityName, String stage,
            String customerName, String lastActivity) {
        return generateSalesAdvice(opportunityName, stage, customerName, lastActivity, null, null, null);
    }

    public String generateSalesAdvice(String opportunityName, String stage,
            String customerName, String lastActivity, String model, String baseUrl, String apiKey) {

        if (!aiService.isAvailable()) {
            return "AI服务暂不可用，建议保持客户跟进。";
        }

        String prompt = String.format(
            "作为销售顾问，请为以下商机提供一条 actionable 的销售建议（50字以内）：\n" +
            "商机：%s\n" +
            "阶段：%s\n" +
            "客户：%s\n" +
            "最近活动：%s",
            opportunityName, stage, customerName, lastActivity
        );

        java.util.Map<String, Object> options = new java.util.LinkedHashMap<String, Object>();
        options.put("temperature", 0.7);
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
}
