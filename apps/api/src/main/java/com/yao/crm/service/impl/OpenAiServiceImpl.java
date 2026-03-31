package com.yao.crm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.config.AiConfig;
import com.yao.crm.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * OpenAI GPT 实现
 */
@Service
@Slf4j
public class OpenAiServiceImpl implements AiService {

    private final AiConfig aiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiServiceImpl(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String generateText(String prompt) {
        return generateText(prompt, Map.of("temperature", 0.7));
    }

    @Override
    public String generateText(String prompt, Map<String, Object> options) {
        if (aiConfig.getOpenai().getApiKey() == null || aiConfig.getOpenai().getApiKey().isEmpty()) {
            log.warn("OpenAI API key not configured");
            return "AI服务未配置，请联系管理员配置 OpenAI API Key";
        }

        try {
            String url = aiConfig.getOpenai().getBaseUrl() + "/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + aiConfig.getOpenai().getApiKey());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiConfig.getOpenai().getModel());
            requestBody.put("messages", new Object[] { Map.of("role", "user", "content", prompt) });
            requestBody.put("temperature", options.getOrDefault("temperature", 0.7));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return parseOpenAiResponse(response.getBody());

        } catch (Exception e) {
            log.error("OpenAI API call failed", e);
            return "AI服务调用失败: " + e.getMessage();
        }
    }

    private String parseOpenAiResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                return message.path("content").asText();
            }
            return response;
        } catch (Exception e) {
            log.warn("Failed to parse OpenAI response: {}", response);
            return response;
        }
    }

    @Override
    public boolean isAvailable() {
        return aiConfig.getOpenai().getApiKey() != null
                && !aiConfig.getOpenai().getApiKey().isEmpty();
    }
}
