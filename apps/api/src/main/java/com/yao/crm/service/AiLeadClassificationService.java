package com.yao.crm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * AI 线索分类服务
 */
@Service
public class AiLeadClassificationService {
    private static final Logger log = LoggerFactory.getLogger(AiLeadClassificationService.class);

    @Autowired
    private AiService aiService;

    /**
     * 预测线索转化概率
     */
    public double predictConversionProbability(String leadName, String company,
            String source, String industry, String description) {

        if (!aiService.isAvailable()) {
            return 50.0; // 默认50%
        }

        String prompt = String.format(
            "预测以下线索的转化可能性（返回 0-100 的数字）：\n" +
            "线索名称：%s\n" +
            "公司：%s\n" +
            "来源渠道：%s\n" +
            "行业：%s\n" +
            "描述：%s",
            leadName, company, source, industry,
            description != null ? description : "无"
        );

        try {
            String result = aiService.generateText(prompt, Map.of("temperature", 0.3));
            return parseProbability(result);
        } catch (Exception e) {
            log.error("Failed to predict conversion probability", e);
            return 50.0;
        }
    }

    /**
     * 推荐线索来源分类
     */
    public String recommendSourceCategory(String company, String website, String description) {

        if (!aiService.isAvailable()) {
            return "DIRECT"; // 默认直线索
        }

        String prompt = String.format(
            "为以下线索推荐最合适的来源分类（返回以下之一：DIRECT, CAMPAIGN, REFERRAL, PARTNER, EVENT, SOCIAL, OTHER）：\n" +
            "公司：%s\n" +
            "网站：%s\n" +
            "描述：%s",
            company, website != null ? website : "无",
            description != null ? description : "无"
        );

        try {
            String result = aiService.generateText(prompt, Map.of("temperature", 0.3, "max_tokens", 20));
            return parseSourceCategory(result);
        } catch (Exception e) {
            log.error("Failed to recommend source category", e);
            return "OTHER";
        }
    }

    /**
     * 生成线索质量评分
     */
    public Map<String, Object> assessLeadQuality(String leadName, String company,
            String phone, String email, String description) {

        double score = predictConversionProbability(leadName, company, null, null, description);

        // 基础信息完整性评分
        int completenessScore = 0;
        if (phone != null && !phone.isEmpty()) completenessScore += 25;
        if (email != null && !email.isEmpty()) completenessScore += 25;
        if (company != null && !company.isEmpty()) completenessScore += 25;
        if (description != null && !description.isEmpty()) completenessScore += 25;

        return Map.of(
            "aiScore", score,
            "completenessScore", completenessScore,
            "overallScore", (score * 0.6 + completenessScore * 0.4)
        );
    }

    private double parseProbability(String aiResponse) {
        try {
            String numStr = aiResponse.replaceAll("[^0-9.]", "");
            double value = Double.parseDouble(numStr);
            return Math.min(100, Math.max(0, value));
        } catch (Exception e) {
            return 50.0;
        }
    }

    private String parseSourceCategory(String aiResponse) {
        String upper = aiResponse.toUpperCase();
        if (upper.contains("DIRECT")) return "DIRECT";
        if (upper.contains("CAMPAIGN")) return "CAMPAIGN";
        if (upper.contains("REFERRAL")) return "REFERRAL";
        if (upper.contains("PARTNER")) return "PARTNER";
        if (upper.contains("EVENT")) return "EVENT";
        if (upper.contains("SOCIAL")) return "SOCIAL";
        return "OTHER";
    }
}
